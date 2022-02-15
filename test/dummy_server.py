import os
from flask import Flask, Response, flash, request, redirect


app = Flask(__name__)

@app.route("/db/resources/json", methods=['GET', 'POST'])
def broker():

    # This is a test endpoint used to check if the server is running
    return Response("<h1>RESOURCE ALIVE</h1>",status=200, mimetype='text/html')


@app.route("/db/subscribers/1.1.1.1/json", methods=['GET', 'POST'])
def res1():

    # This is a test endpoint used to check if the server is running
    return Response("<h1>GET</h1>",status=200, mimetype='text/html')


@app.route("/db/subscribers/1.1.1.2/json", methods=['GET', 'POST'])
def res2():

    # This is a test endpoint used to check if the server is running
    return Response("<h1>GET</h1>",status=200, mimetype='text/html')


@app.route("/db/subscribers/1.1.1.3/json", methods=['GET', 'POST'])
def res3():

    # This is a test endpoint used to check if the server is running
    return Response("<h1>GET</h1>",status=200, mimetype='text/html')


app.run(host="127.0.0.2")
