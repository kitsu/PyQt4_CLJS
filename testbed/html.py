"""Some composable HTML functions, and pre-built widgets."""
from functools import partial
from itertools import chain

#---------------------------- Generic HTML ------------------------------------

self_closing = ["area", "base", "br", "col", "command", "embed", "hr", "img",
                "input", "keygen", "link", "meta", "param", "source", "track",
                "wbr"]

def tag(kind, *args, **kwargs):
    """Generic tag-producing function."""
    kind = kind.lower()
    result = list()
    result.append("<{}".format(kind))
    for key, val in kwargs.items():
        if key in ('kind', '_type'):
            key = 'type'
        elif key in ('klass', 'cls'):
            key = 'class'
        result.append(" {}=\"{}\"".format(key, val))
    if kind in self_closing:
        result.append(" />")
    else:
        result.append(">{}</{}>\n".format("".join(args), kind))
    return "".join(result)

# Misc simple tags
style = partial(tag, 'style')
div = partial(tag, 'div')
span = partial(tag, 'span')

# Form related tags
label = partial(tag, 'label')
textbox = partial(tag, 'input', _type='text')
button = partial(tag, 'button', _type='button')

def html(head='', body='', **body_attrs):
    """Root document element."""
    return tag('html', tag('head', head), tag('body', body, **body_attrs))

def css(url):
    """A css link tag."""
    return tag('link', href=url, rel='stylesheet', _type='text/css')

js = partial(tag, 'script', _type='text/javascript')
def script(url=None, script=None):
    """A script tag."""
    if url:
        return js(src=url)
    elif script:
        return js(script)

#--------------------------- Lister Specific ----------------------------------

# Doclist filter input controls
docfilter = div("&nbsp", label('Filter:'), "&nbsp",
                textbox(id='filter_input', autofocus='autofocus'),
                button("Clear", id='filter_clear'),
                id='docfilter')

bar = span('|', cls='bar noselect')

def cell(idx, contents):
    """Create html representing a cell in a row."""
    return span("&nbsp{}".format(contents),
                cls="col{} noselect".format(idx))

def add_bars(data):
    """Transform column data into cell tags, and add separating bars."""
    cells = [(cell(i, c), bar) for i, c in enumerate(data)]
    cells = list(chain(*cells))
    cells.pop() # Remove last vertical bar
    return cells

def row(idx, data):
    """Create html representing a row of cells."""
    cells = add_bars(data)
    # Setup row classes
    classes = "row noselect"
    if idx%2 == 1:
        classes += " zebra"
    return div(*cells, id="row{}".format(idx), cls=classes)

def header(columns):
    """Create html for column headers."""
    cells = add_bars(columns)
    return div(docfilter, div(*cells, cls='noselect'), id='header')

def doclist(headers, data):
    """Create a complete doclist from headers and 2d data."""
    rows = [header(headers)]
    rows.extend([row(i, r) for i, r in enumerate(data)])
    return div(*rows, id='doclist')

if __name__ == '__main__':
    head = ['first', 'second', 'third']
    data = [range(3), range(3, 6), range(6, 9)]
    with open('test.html', 'w') as outfile:
        outfile.write(html(body=doclist(head, data)))
    
