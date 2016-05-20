# ClojureScript in PyQt4 Testbed

Embedding CLJS output into a PyInstaller generated executable.

## Overview

This is an experiment in extending ClojureScript's reach into the desktop space.
This is specifically in support of an existing Python project I'm writing, but
but should be extendable to arbitrary ClojureScript applications.

## Setup [FIXME]

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## License

Copyright © 2016 Edward Blake

Distributed under the BSD simplified 3-clause license
