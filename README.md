# macrophage
This **IntelliJ IDEA** plugin applies given macro to every item in the Find results

Usage:

* Record a macro;
* Find some lines. Make them appear in the Find results area;
* `Ctrl+Alt+F3` to launch macrophage. It'll prompt you for a macro to apply;
* Watch IDEA works for you. Touch nothing cause macros doesn't work in background and any other action may break the sequence of actions.

There is also an action to navigate to the next item in the Find results (`Ctrl+Shift+F3`)
Macrophage just triggers this action and then evals a macro in cycle.

*I suggest you commit before applying macrophage. If something goes wrong the code may easily become corrupted.*
