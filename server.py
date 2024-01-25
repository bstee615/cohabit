import os
import json
from datetime import date, timedelta, datetime
from flask import Flask, render_template

app = Flask(__name__)

@app.route('/')
def home():
   return render_template('index.html')

FNAME = "database.json"
DATE_FORMAT = '%Y-%m-%d'

def read_database():
    if not os.path.exists(FNAME):
        with open(FNAME, "w") as f:
            f.write("[]")
    try:
        with open(FNAME) as f:
            data = json.load(f)
    except json.decoder.JSONDecodeError:
        with open(FNAME, "w") as f:
            f.write("[]")
    with open(FNAME) as f:
        data = json.load(f)
    for d in data:
        d["date"] = datetime.strptime(d["date"], DATE_FORMAT).date()
    print("Loaded database", data)
    return data

def write_database(data):
    with open(FNAME, "w") as f:
        if len(data) == 0:
            f.write("[]")
        else:
            for d in data:
                d["date"] = datetime.strftime(d["date"], DATE_FORMAT)
            json.dump(data, f)

def get_today():
    return date.today()

def get_days_in_a_row(data):
    # newest to oldest
    days_in_a_row = 0
    today = get_today()
    for record in reversed(data):  
        if record["date"] == today:
            days_in_a_row += 1
            today = today - timedelta(days=1)
    if days_in_a_row == 0:
        today = get_today() - timedelta(days=1)
        for record in reversed(data):  
            if record["date"] == today:
                days_in_a_row += 1
                today = today - timedelta(days=1)
    return days_in_a_row

def get_status(db):
    lis = "".join(f"""<li>{datetime.strftime(record["date"], DATE_FORMAT)}</li>""" for record in db)
    message = "Yay us"
    days = get_days_in_a_row(db)
    if days == 0:
        message = "Shame"
    suffix = ""
    if len(db) > 0 and db[-1]["date"] != get_today():
        suffix = " Do it today!"
    return f"""<div>{message}! We've kept our habit up for the last {days} days.{suffix}</div><div class="box">Did it on:<ul class="f-col dense" role="list">{lis}</ul></div>"""

@app.route("/count", methods = ["GET"])
def count():
    db = read_database()
    return get_status(db)

@app.route("/delete", methods = ["POST"])
def clicked_decrement():
    db = read_database()

    today = get_today()
    index = next((i for i, record in enumerate(db) if record["date"] == today), None)
    if index is not None:
        db.pop(index)

    response = get_status(db)
    write_database(db)
    return response

@app.route("/add", methods = ["POST"])
def clicked_increment():
    db = read_database()

    today = get_today()
    if not any(record["date"] == today for record in db):
        db.append({"date": today})

    response = get_status(db)
    write_database(db)
    return response

if __name__ == '__main__':
    app.run("0.0.0.0", port="5000")
