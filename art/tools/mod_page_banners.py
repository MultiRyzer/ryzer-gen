"""Renders the mod page's banners (art/modpage/banners.html) to PNGs in art/gallery/page/.

CurseForge and Modrinth descriptions are Markdown: no fonts and no styles. So the page's look (the
design system's Chakra Petch headings, graphite and cyan) goes on as pictures, and the words that
matter stay as text beneath them. Each banner is a div in banners.html; this serves the art folder
locally, asks Edge (headless) for each banner's size, then screenshots each at that size.

Needs Microsoft Edge (or Chrome) and a network connection for the Google fonts.

    python art/tools/mod_page_banners.py [banner ...]
"""
import functools
import http.server
import os
import re
import shutil
import subprocess
import sys
import tempfile
import threading

ART = os.path.normpath(os.path.join(os.path.dirname(__file__), '..'))
OUT = os.path.join(ART, 'gallery', 'page')
BROWSERS = [
    r'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    r'C:\Program Files\Microsoft\Edge\Application\msedge.exe',
    r'C:\Program Files\Google\Chrome\Application\chrome.exe',
    'msedge', 'google-chrome', 'chromium',
]


def browser():
    for b in BROWSERS:
        if os.path.exists(b) or shutil.which(b):
            return b
    sys.exit('No Edge or Chrome found')


def serve():
    """Serves the art folder on a free local port, so the page can load its images and fonts."""
    class Quiet(http.server.SimpleHTTPRequestHandler):
        def log_message(self, *args):
            pass

    handler = functools.partial(Quiet, directory=ART)
    server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server


def run(exe, profile, *args):
    return subprocess.run([exe, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=1',
                           f'--user-data-dir={profile}', '--virtual-time-budget=8000', *args],
                          capture_output=True, text=True, timeout=120)


def main():
    exe = browser()
    server = serve()
    base = f'http://127.0.0.1:{server.server_port}/modpage/banners.html'
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as profile:
        dom = run(exe, profile, '--dump-dom', base).stdout
        found = re.search(r'<pre id="sizes" hidden="">(.*?)</pre>', dom, re.S)
        if not found:
            sys.exit('The page did not report its banner sizes (did the fonts load?)')
        sizes = [line.split() for line in found.group(1).strip().splitlines()]
        wanted = set(sys.argv[1:])
        for name, w, h in sizes:
            if wanted and name not in wanted:
                continue
            path = os.path.join(OUT, f'{name}.png')
            run(exe, profile, f'--screenshot={path}', f'--window-size={w},{h}', f'{base}?b={name}')
            print(f'{name}: {w} x {h}' if os.path.exists(path) else f'{name}: FAILED')
    server.shutdown()


if __name__ == '__main__':
    main()
