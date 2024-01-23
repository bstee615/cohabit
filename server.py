import os
from flask import Flask, render_template

app = Flask(__name__)

@app.route('/')
def home():
   return render_template('index.html')

@app.route("/count", methods = ["GET"])
def count():
    FNAME = "database.txt"
    if os.path.exists(FNAME):
        with open(FNAME, "r") as f:
            times_clicked = int(f.read())
    else:
            times_clicked = 0

    return f"Great job! We've kept our habit up for {times_clicked} days."

@app.route("/decrement", methods = ["POST"])
def clicked_decrement():
    FNAME = "database.txt"
    if os.path.exists(FNAME):
        with open(FNAME, "r") as f:
            times_clicked = int(f.read())
    else:
            times_clicked = 0

    times_clicked -= 1
    
    with open(FNAME, "w") as f:
       f.write(str(times_clicked))

    return f"Great job! We've kept our habit up for {times_clicked} days."

@app.route("/increment", methods = ["POST"])
def clicked_increment():
    FNAME = "database.txt"
    if os.path.exists(FNAME):
        with open(FNAME, "r") as f:
            times_clicked = int(f.read())
    else:
            times_clicked = 0

    times_clicked += 1
    
    with open(FNAME, "w") as f:
       f.write(str(times_clicked))

    return f"Great job! We've kept our habit up for {times_clicked} days."

if __name__ == '__main__':
   app.run()
