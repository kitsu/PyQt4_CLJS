# ClojureScript in PyQt4 Testbed

Embedding CLJS output into a PyInstaller generated executable.

## Overview

This is an experiment in extending ClojureScript's reach into the desktop space.
This is specifically in support of an existing Python project I'm writing, but
but should be extendable to arbitrary ClojureScript applications.

## Setup

This requires Python 2.7 with the PyQt4 module installed. On Windows I recommend
[Anaconda Python](https://www.continuum.io/downloads). Python must be on your
path for the batch scripts to work.

This thing also uses [Leiningen](https://www.leiningen.org) to manage Clojure
dependencies and perform ClojureScript builds. Lein must be on your path for the
batch scripts to work. I also have `LEIN_JAVA_CMD` defined and java bin on my
path.

The `run.cmd` batch script does a single ClojureScript build without advanced
compilation, then runs the Python app from source. During development I just run
`lein cljsbuild auto` in one shell window and manually run
`python .\testbed\app.py` in another.

The `build.cmd` batch script requires [PyInstaller](http://www.pyinstaller.org/)
version >= v3.0. Note: at this time PyInstaller 3 is not in the conda package
archives. You will need to install using pip or with a manual installer.

Building will compile ClojureScript with advanced optimizations and then bundle
everything into a single executable in a _dist_ folder.

## License

Copyright © 2016 Edward Blake

Distributed under the BSD simplified 3-clause license
