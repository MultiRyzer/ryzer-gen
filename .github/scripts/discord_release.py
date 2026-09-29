"""Posts a GitHub release's notes to Discord through a webhook.

Discord caps a message at 2,000 characters, so the notes are split into several messages, cut
between sections where they can be (before a heading) and between lines where they must. Link
previews are turned off, so the notes are not buried under embeds.

Reads RELEASE_NAME, RELEASE_URL, RELEASE_BODY and DISCORD_WEBHOOK from the environment.
"""
import json
import os
import time
import urllib.error
import urllib.request

LIMIT = 1900  # under Discord's 2,000, with room to spare
SUPPRESS_EMBEDS = 1 << 2


def sections(text):
    """The notes as blocks, each starting at a heading (the first may not)."""
    blocks, current = [], []
    for line in text.splitlines():
        if line.startswith('#') and current:
            blocks.append('\n'.join(current).strip('\n'))
            current = []
        current.append(line)
    if current:
        blocks.append('\n'.join(current).strip('\n'))
    return [b for b in blocks if b.strip()]


def by_lines(block):
    """A block too long for one message, cut between lines."""
    parts, current = [], ''
    for line in block.splitlines():
        while len(line) > LIMIT:
            if current:
                parts.append(current)
                current = ''
            parts.append(line[:LIMIT])
            line = line[LIMIT:]
        if current and len(current) + 1 + len(line) > LIMIT:
            parts.append(current)
            current = line
        else:
            current = f'{current}\n{line}' if current else line
    if current:
        parts.append(current)
    return parts


def messages(text):
    """The notes packed into as few messages as fit, whole sections where possible."""
    out, current = [], ''
    for block in sections(text):
        pieces = [block] if len(block) <= LIMIT else by_lines(block)
        for piece in pieces:
            if current and len(current) + 2 + len(piece) > LIMIT:
                out.append(current)
                current = piece
            else:
                current = f'{current}\n\n{piece}' if current else piece
    if current:
        out.append(current)
    return out


def post(webhook, content):
    data = json.dumps({'content': content, 'flags': SUPPRESS_EMBEDS,
                       'allowed_mentions': {'parse': []}}).encode()
    request = urllib.request.Request(webhook, data=data, method='POST', headers={
        'Content-Type': 'application/json', 'User-Agent': 'ryzer-gen-release-notes'})
    for _ in range(5):
        try:
            urllib.request.urlopen(request).close()
            return
        except urllib.error.HTTPError as e:
            if e.code != 429:
                raise
            # Rate limited: wait as long as Discord asks, then try again.
            time.sleep(float(json.loads(e.read() or b'{}').get('retry_after', 2)))
    raise RuntimeError('Discord kept rate limiting the webhook')


def main():
    name = os.environ['RELEASE_NAME']
    url = os.environ['RELEASE_URL']
    body = os.environ.get('RELEASE_BODY', '').replace('\r\n', '\n')
    text = f'# {name}\nDownload: <{url}>\n\n{body}'
    webhook = os.environ['DISCORD_WEBHOOK']
    for message in messages(text):
        post(webhook, message)
        time.sleep(1)


if __name__ == '__main__':
    main()
