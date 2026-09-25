"""Searches fission station core layouts for the best power and the best fuel economy, using the
same rules as StationReactor.java (keep them in step). The best power it finds for each fuel is the
screen's rating reference (StationReactor.BEST_URANIUM and BEST_MOX). Run:
    python art/tools/core_optimiser.py

Channels: F fuel, M moderator (graphite), R control rod, C coolant.
"""
import random, math, sys
G=5; N=25; CAP=3000
BASE={'U':1000,'X':2000}
# Coolant capacity multiplier per fuel: an all-MOX core's channels carry twice as much.
COOL={'U':1,'X':2}
def nb(i):
    x,z=i%G,i//G; out=[]
    if x>0: out.append(i-1)
    if x<G-1: out.append(i+1)
    if z>0: out.append(i-G)
    if z<G-1: out.append(i+G)
    return out
NB=[nb(i) for i in range(N)]
def analyse(L, fuel):
    heat=[0.0]*N; burn=[0.0]*N
    for i,t in enumerate(L):
        if t!='F': continue
        h=1; b=1
        for n in NB[i]:
            k=L[n]
            if k=='M': h+=0.4
            elif k=='F': h+=0.2; b+=0.2
            elif k=='R': h-=0.35; b-=0.35
        heat[i]=BASE[fuel]*max(0.1,h); burn[i]=max(0.1,b)
    load={}
    stranded=0
    for i in range(N):
        if heat[i]==0: continue
        cs=[n for n in NB[i] if L[n]=='C']
        if not cs: stranded+=heat[i]; continue
        for c in cs: load[c]=load.get(c,0)+heat[i]/len(cs)
    over=sum(max(0,l-CAP*COOL[fuel]) for l in load.values())
    gen=sum(heat)
    used=len(load)
    r = gen/(used*CAP*COOL[fuel]) if used else 0
    return gen, stranded+over, r, sum(burn), sum(1 for t in L if t=='F')
def score(L, fuel, mode):
    gen,stuck,r,burn,rods=analyse(L,fuel)
    if gen==0 or stuck>0: return 0,None
    T=150+450*r; eff=0.25+0.15*min(1,(T-150)/450)
    out=gen*eff
    # mode 'power': raw output. mode 'economy': output per rod burn, needing at least 2000 FE/t
    val = out if mode=='power' else (out/burn if out>=2000 else 0)
    return val,(out,gen,r,T,eff,burn,rods)
def search(fuel, mode, iters=240000, seed=1):
    random.seed(seed); best=(0,None,None)
    for restart in range(16):
        L=[random.choice('CFMR') for _ in range(N)]
        cur=score(L,fuel,mode)[0]; temp=300 if mode=='power' else 30
        for k in range(iters//16):
            i=random.randrange(N); old=L[i]; L[i]=random.choice('CFMR')
            v=score(L,fuel,mode)[0]
            if v>=cur or random.random()<math.exp((v-cur)/max(temp,1e-6)): cur=v
            else: L[i]=old
            temp*=0.9996
            if cur>best[0]: best=(cur,L[:],score(L,fuel,mode)[1])
    return best
if __name__ == "__main__":
    for fuel in ('U', 'X'):
        for mode in ('power', 'economy'):
            v, L, info = search(fuel, mode)
            out, gen, r, T, eff, burn, rods = info
            print(f"{'uranium' if fuel == 'U' else 'MOX'} / best {mode}: {out:.0f} FE/t, {rods} rods, T {T:.0f}C, "
                  f"eff {eff * 100:.1f}%, FE/t per rod-burn {out / burn:.0f}")
            for z in range(G):
                print('    ', ' '.join(L[z * G:(z + 1) * G]))
