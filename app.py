from flask import Flask, render_template

app = Flask(__name__)


@app.get("/")
def home() -> str:
    return render_template("index.html")


@app.get("/hello")
def hello_python() -> tuple[str, int]:
    return "Hello from Python route!", 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
