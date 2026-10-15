"""Posts a GitHub release's notes to Discord through a webhook, as #announcements cards.

Each release is one cyan card (an embed) titled with its version and linked to the release, dated
with its publish time. The text before the first heading is the card's summary, and each section
of the notes (### New, ### Changed, ### Fixed ...) becomes a block of it, its bullets as dots. Notes
too long for one card carry on in more cards, cut between sections where they can be and between
bullets where they must. Nobody is pinged.

Reads RELEASE_TAG, RELEASE_URL, RELEASE_BODY, RELEASE_DATE (ISO 8601) and DISCORD_WEBHOOK from the
environment. With DRY_RUN set, prints the messages instead of posting them.
"""
import json
import os
import time
import urllib.error
import urllib.request

CYAN = 0x35C8F5
FIELD = 1024          # Discord's limit for one block of a card
DESCRIPTION = 4096    # for a card's summary
CARD = 5800           # under Discord's 6,000 characters for a card, with room to spare
FIELDS = 25           # blocks in one card


def parse(body):
    """The summary (text before the first heading) and the sections, as (heading, lines)."""
    summary, sections, current = [], [], None
    for raw in body.replace('\r\n', '\n').splitlines():
        line = raw.rstrip()
        if line.startswith('#'):
            current = (line.lstrip('#').strip(), [])
            sections.append(current)
        elif current is None:
            summary.append(line)
        elif line.strip():
            current[1].append(line)
    return '\n'.join(summary).strip(), sections


def bullet(line):
    """A Markdown list item as a dot (nested items indented); other lines as they are."""
    stripped = line.lstrip()
    indent = len(line) - len(stripped)
    if stripped.startswith(('- ', '* ')):
        return (' ' if indent else '') + '• ' + stripped[2:]
    return stripped


def chunks(text, limit):
    """Text cut into pieces no longer than limit, between lines where possible."""
    out, current = [], ''
    for line in text.split('\n'):
        while len(line) > limit:
            if current:
                out.append(current)
                current = ''
            cut = line.rfind(' ', 0, limit)
            cut = cut if cut > 0 else limit
            out.append(line[:cut])
            line = line[cut:].lstrip()
        if current and len(current) + 1 + len(line) > limit:
            out.append(current)
            current = line
        else:
            current = f'{current}\n{line}' if current else line
    if current:
        out.append(current)
    return out


def cards(title, url, date, body):
    """The release as cards: the first titled, linked and dated, the rest carrying on."""
    summary, sections = parse(body)
    fields = []
    for heading, lines in sections:
        text = '\n'.join(bullet(line) for line in lines)
        for i, piece in enumerate(chunks(text, FIELD)):
            fields.append({'name': heading if i == 0 else '​', 'value': piece})
    first = {'title': title, 'url': url, 'color': CYAN, 'footer': {'text': 'Ryzer Gen release notes'}}
    if date:
        first['timestamp'] = date
    out, card, size = [], first, len(title) + 24
    if summary:
        card['description'] = chunks(summary, DESCRIPTION)[0]
        size += len(card['description'])
    for field in fields:
        cost = len(field['name']) + len(field['value'])
        if card.get('fields') and (size + cost > CARD or len(card['fields']) >= FIELDS):
            out.append(card)
            card, size = {'color': CYAN}, 0
        card.setdefault('fields', []).append(field)
        size += cost
    out.append(card)
    return out


def post(webhook, embed):
    data = json.dumps({'embeds': [embed], 'allowed_mentions': {'parse': []}}).encode()
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
    tag = os.environ['RELEASE_TAG']
    title = 'Ryzer Gen ' + tag.removeprefix('v')
    embeds = cards(title, os.environ['RELEASE_URL'], os.environ.get('RELEASE_DATE') or None,
                   os.environ.get('RELEASE_BODY', ''))
    if os.environ.get('DRY_RUN'):
        print(json.dumps(embeds, indent=2, ensure_ascii=False))
        return
    webhook = os.environ['DISCORD_WEBHOOK']
    for embed in embeds:
        post(webhook, embed)
        time.sleep(1)


if __name__ == '__main__':
    main()
