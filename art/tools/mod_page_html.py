"""Writes docs/MOD-PAGE.html: the mod page (docs/MOD-PAGE.md, below its first ---) as plain HTML.

CurseForge's normal description editor does not read Markdown, but its source view takes HTML. This
covers only what the page uses: images, links, bold, code, bullets, quotes, headings, paragraphs
and the centred block at the top.

    python art/tools/mod_page_html.py
"""
import html
import os
import re

DOCS = os.path.normpath(os.path.join(os.path.dirname(__file__), '..', '..', 'docs'))


def inline(s):
    s = html.escape(s, quote=False)
    s = re.sub(r'\[!\[([^\]]*)\]\(([^)]+)\)\]\(([^)]+)\)', r'<a href="\3"><img src="\2" alt="\1"></a>', s)
    s = re.sub(r'!\[([^\]]*)\]\(([^)]+)\)', r'<img src="\2" alt="\1">', s)
    s = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'<a href="\2">\1</a>', s)
    s = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', s)
    s = re.sub(r'`([^`]+)`', r'<code>\1</code>', s)
    return s


def convert(text):
    out, para, items = [], [], []
    centred = False

    def flush():
        if para:
            style = ' style="text-align: center;"' if centred else ''
            out.append(f'<p{style}>' + '<br>\n'.join(inline(p) for p in para) + '</p>')
            para.clear()
        if items:
            out.append('<ul>\n' + '\n'.join(f'<li>{inline(i)}</li>' for i in items) + '\n</ul>')
            items.clear()

    for line in text.splitlines():
        stripped = line.strip()
        if stripped in ('<center>', '</center>'):
            flush()
            centred = stripped == '<center>'
        elif not stripped:
            flush()
        elif line.startswith('## '):
            flush()
            out.append(f'<h2>{inline(line[3:])}</h2>')
        elif line.startswith('- '):
            items.append(line[2:])
        elif line.startswith('> '):
            flush()
            out.append(f'<blockquote><p>{inline(line[2:])}</p></blockquote>')
        else:
            para.append(line)
    flush()
    return '\n\n'.join(out) + '\n'


def main():
    text = open(os.path.join(DOCS, 'MOD-PAGE.md'), encoding='utf-8').read()
    page = text.split('\n---\n', 1)[1]
    with open(os.path.join(DOCS, 'MOD-PAGE.html'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(convert(page))
    print('wrote docs/MOD-PAGE.html')


if __name__ == '__main__':
    main()
