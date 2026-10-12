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
- **Fix mistakes in one click.** If a symbol is misread, click it and choose the right one.
  The answer updates and the calculator learns from the correction.
- **Learns your handwriting.** A built-in practice window collects samples of your writing
  and retrains the model.
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
2. Click **Calculate**. A trailing `=` is optional.
3. If something was misread, click that symbol under the canvas and choose the right one.
4. Use **Undo** and **Clear** to fix your writing, and **History…** to see earlier
   calculations. Double-click an entry, or select it and click **Load onto canvas**, to put
   it back on the canvas.

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

Corrections you make in the calculator are saved as samples and learned straight away.

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
   `÷`. A fraction bar or root sign is kept separate from the numbers around it.
2. **Recognition** classifies each symbol with a small neural network. A tiny mark on the
   baseline is read as a decimal point.
3. **Layout** uses the position of each symbol to find fractions, roots and powers, and
   turns the expression into a single line such as `(3÷4)+2^3`. Common look-alikes in
   function names are fixed automatically, so a misread `c05` still becomes `cos`.
4. **Evaluation** calculates the result.

### Project structure

The code follows Clean Architecture, under `src/main/java/handcalc`:

- `entity`: core types such as strokes, glyphs, expressions and history entries
- `use_case`: recognising and solving, correcting a symbol, collecting samples, practising
  expressions, training the model, and history
- `interface_adapter`: controllers, presenters and view models
- `view`: the Swing user interface
- `data_access`: the neural network, file storage and synthetic training data
- `app`: wiring and the `Main` entry point
