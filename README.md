# Handwritten Calculator

A desktop calculator you write on. Draw a maths expression with your mouse or trackpad, such
as `12+3×4`, a fraction or `sin(30)`, and it reads your handwriting and gives you the answer.

Recognition runs entirely on your computer with a small neural network that you can train
on your own handwriting. Nothing is sent over the network.

## Features

- **Write naturally.** Digits, `+ - × ÷ =`, brackets, the decimal point, `√`, `π`, and the
  functions `sin`, `cos` and `tan`.
- **2D maths.** Fractions, square roots and powers are worked out from where you write them,
  not just the order you write them in.
- **Fix mistakes in one click.** Rest the mouse on a symbol to see what else it might be, and
  click the right one. It replaces what you wrote and the answer updates.
- **Learns your handwriting.** A built-in practice window collects samples of your writing
  and retrains the model.
- **A page of working.** Write one calculation per line, as on paper. End a line with `=`
  and pause: its answer is written after it in a handwriting typeface, and stays there
  until you change that line or wipe the answer off. A symbol that makes a line wrong is
  underlined in red.
- **Check what was calculated.** Rest the mouse on an answer to see the calculation typeset
  the way it was read: fractions, powers and roots included.
- **Zoom and move around.** Pinch with two fingers to zoom, and swipe with two fingers to
  move the page in any direction.
- **Eraser and undo.** The eraser wipes whole strokes; Undo takes back the last stroke,
  eraser sweep or clear.
- **History.** Every calculation is saved with its handwriting, so you can load an old one
  back onto the canvas.

## Getting started

You need **Java 11 or newer** and **Maven**.

```bash
git clone https://github.com/Mjff778/JavaCalc.git
cd JavaCalc
mvn exec:java
```

To run the test suite:

```bash
mvn test
```

## Using the calculator

1. Write an expression on the canvas. Leave a small gap between symbols: strokes that touch
   are read as a single symbol.
2. End the line with `=` and lift the pen. After a moment the answer is written after the
   `=` (or `undefined`, for something like `5÷0`). Write the next calculation on a new line
   below; each line is worked out on its own. Changing a line works it out again; the
   eraser wipes an answer off, and Undo brings it back.
3. Rest the mouse on an answer to check the calculation that was done. If a symbol is
   underlined in red, the line cannot be read that way: rest the mouse on the symbol to see
   how it was read and choose what it really is, or rewrite it. Any symbol of a finished
   line can be corrected this way, underlined or not: the symbol you pick takes the place of
   what you wrote, set in a handwriting typeface, and is always read as you picked it. The
   choices only appear once you have stopped writing for a moment, so they never get in the
   way of the pen. Picking a symbol does not teach the calculator your handwriting, so it is
   safe to use for changing a sum; use **Teach it my handwriting…** for that.
4. Use **Eraser**, **Undo** and **Clear** to fix your writing, and **History…** to see
   earlier calculations. Double-click an entry, or select it and click **Load onto canvas**, to put
   it back on the canvas.

To move around the page, swipe with two fingers (or use the scroll wheel; hold Shift to
scroll sideways). To zoom, pinch, or hold ⌘ (Ctrl on Windows and Linux) and scroll;
⌘+, ⌘− and ⌘0 zoom in, out and back. Pinch-to-zoom needs the Mac gesture API, which
`.mvn/jvm.config` makes available to `mvn exec:java`; when running from an IDE, add
`--add-exports java.desktop/com.apple.eawt.event=ALL-UNNAMED` to the VM options.

Angles are in degrees, unless the angle contains `π`, in which case it is in radians
(`sin(π÷2)` is 1). `π` also multiplies without a sign, so `2π` works.

### Writing tips

- **Powers:** write the exponent smaller and at the upper right of its base, clearly above
  the base's middle.
- **Fractions:** write the numerator above the bar and the denominator below it.
- **Roots:** draw the `√` sign and write the number under or next to it.
- **Division and equals:** `÷` is a bar with a dot above and below it, and `=` is two
  stacked bars.

## Training it on your handwriting

The model starts from built-in synthetic samples, so it is only a rough match for real
handwriting at first. A few minutes of training makes a big difference:

1. Click **Teach it my handwriting…**.
2. In **Single symbols** mode, write the symbol shown and press **Save & next** (or Enter).
   It always asks for the symbol you have practised least. About 20 samples per symbol is a
   good target, and a symbol turns green when it reaches that. Digits and operators matter
   most.
3. Or switch to **Whole expressions**. You write a generated expression and every symbol in
   it becomes a training sample. Write the symbols left to right and keep them apart.
4. Click **Train model**. It reports its accuracy on samples held back from training and
   switches the calculator to the new model straight away.

Whenever samples are added, the model also retrains itself on all of them in the background
a moment later. Picking a symbol in the calculator only changes that symbol: it does not
train the model, so changing a sum never teaches it the wrong thing.

## Your data

Everything is stored locally in the `data/` folder:

| File | What it is |
|---|---|
| `data/samples.txt` | All your handwriting samples, one per line. This is what the model learns from. |
| `data/model.bin` | The trained model. It is a cache: it is rebuilt from the samples whenever it is missing or out of date. |
| `data/history.jsonl` | Your calculation history. It is listed in `.gitignore`, so it stays on your computer. |

To take your training to another computer, copy `data/samples.txt`.

## How it works

1. **Segmentation** groups pen strokes into symbols. Strokes that cross or nearly touch
   belong together (as in `+` or `4`), and stacked bars and dots are combined into `=` and
   `÷`, as long as nothing is written between the bars. A fraction bar or root sign is kept
   separate from the numbers around it.
2. **Lines** are found from where the symbols sit on the page; each line is read and solved
   on its own, once it ends in `=`.
3. **Recognition** classifies each symbol with a small neural network. A tiny mark on the
   baseline is read as a decimal point, and a sign that might be a root and has something
   written under it is a root. Symbols you picked are read as picked.
4. **Layout** uses the position of each symbol to find fractions, roots and powers, and
   turns the expression into a single line such as `(3÷4)+2^3`. Common look-alikes in
   function names are fixed automatically, so a misread `c05` still becomes `cos`. A symbol
   that makes the line impossible to read is traced back to the handwriting and underlined.
5. **Evaluation** calculates the result, which is written after the `=` at the size of your
   digits.

### Project structure

The code follows Clean Architecture, under `src/main/java/handcalc`:

- `entity`: core types such as strokes, glyphs, expressions and history entries
- `use_case`: recognising and solving, correcting a symbol, collecting samples, practising
  expressions, training the model, and history
- `interface_adapter`: controllers, presenters and view models
- `view`: the Swing user interface
- `data_access`: the neural network, file storage and synthetic training data
- `app`: wiring and the `Main` entry point

The answers are written in [Caveat](https://github.com/googlefonts/caveat), included under
the SIL Open Font License (`src/main/resources/fonts/Caveat-OFL.txt`).
