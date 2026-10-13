package com.ryzer.ryzergen.machine;

/** A machine block entity whose moving parts, glows and lamp client.MachineMotion draws. */
public interface AnimatedMachine {
    MachineClock clock();

    /** The name its moving parts are listed under in MachineMotion (the machine's id). */
    String motionId();
}
