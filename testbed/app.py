"""Minimal Python container app."""
import sys, os
from PyQt4 import QtGui, QtCore
from PyQt4.QtWebKit import QWebView, QWebSettings, QWebInspector
qt = QtCore.Qt

import html

# Enable developer tools inspector (right click menu)
settings = QWebSettings.globalSettings()
settings.setAttribute(QWebSettings.DeveloperExtrasEnabled, True)

def get_resource_path(resource):
    """Resolves the app path when run as a script or frozen."""
    if hasattr(sys, '_MEIPASS'):
        path = sys._MEIPASS
    else:
        path = os.path.dirname(__file__)
    return os.path.join(path, resource)

class MainWindow(QtGui.QMainWindow):
    """Main application window."""

    def __init__(self, headers, data):
        QtGui.QMainWindow.__init__(self)
        self.setWindowTitle("PyQt Cljs Testbed")
        self.setMaximumWidth(500)

        self.table = TableWidget(headers, data)
        self.setCentralWidget(self.table)

class Proxy(QtCore.QObject):
    """Expose python side callbacks to JavaScript code.

    http://pysnippet.blogspot.com/2010/01/calling-python-from-javascript-in-pyqts.html"""
    def __init__(self, other):
        QtCore.QObject.__init__(self)
        self.other = other

    @QtCore.pyqtSlot(str)
    def log(self, msg):
        print msg

class TableWidget(QWebView):
    """Display tabular data in a webview."""

    def __init__(self, headers, data):
        QWebView.__init__(self)
        # Populate html table
        self.build_page(headers, data)
        # A proxy object is needed to communicate between Python and JavaScript
        self.proxy = Proxy(self)
        self.frame = self.page().mainFrame()
        self.frame.addToJavaScriptWindowObject("proxy", self.proxy)
        # The handle of the inspect window must be held
        self.inspector = None

    def contextMenuEvent(self, event):
        """Replace the default RMB menu."""
        menu = QtGui.QMenu(self)
        inspect = QtGui.QAction('&Inspect', self)
        inspect.triggered.connect(self.show_inspector)
        menu.addAction(inspect)
        QWebView.contextMenuEvent(self, event)

    def build_page(self, headers, data):
        """Build table html containing data rows."""
        table = html.doclist(headers, data)
        styles = self.get_styles()
        scripts = self.get_scripts()
        doc = html.html(styles + scripts, table, cls='noselect')
        basepath = os.path.abspath(os.path.dirname(__file__))
        self.setHtml(doc, QtCore.QUrl.fromLocalFile(basepath))

    def get_styles(self):
        """Create the default style sheet tag and set column widths."""
        styles = list()
        styles.append(html.css(get_resource_path('style.css')))
        # This replaces user specified column widths from config file
        overrides = list()
        cw = ['2em', '3em', 'other']
        cwtmp = "span.col{} {{width: {}}}"
        for i, size in enumerate(cw):
            overrides.append(cwtmp.format(i, size))
        overrides.pop() # Always ignore last value
        styles.append(html.style("\n".join(overrides)))
        return "".join(styles)


    def get_scripts(self):
        """Create the main script tag."""
        scripts = list()
        scripts.append(html.script(url=get_resource_path('main.js')))
        return "".join(scripts)

    def show_inspector(self, event=None):
        """Show the web inspector."""
        if not self.inspector:
            inspect = QWebInspector()
            inspect.setPage(self.page())
            self.inspector = inspect
        self.inspector.show()

if __name__ == '__main__':
    headers = ["No.", "ID", "Name"]
    data = [("1", 'foo_1', 'foo',),
            ("2", 'bar_1', 'bar',),
            ("3", 'bar_2', 'bar',),
            ("4", 'bar_3', 'bar',),
            ("5", 'bar_4', 'bar',),
            ("6", 'bar_5', 'bar',),
            ("7", 'baz_1', 'baz',),
            ("8", 'baz_2', 'baz',),
            ("9", 'baz_3', 'baz',),
            ("10", 'spam_1', 'spam',),
            ("11", 'spam_2', 'spam',)]

    appQt = QtGui.QApplication([sys.argv[0]])
    win = MainWindow(headers, data)
    win.show()
    appQt.exec_()
