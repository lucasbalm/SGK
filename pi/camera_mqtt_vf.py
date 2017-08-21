import RPi.GPIO as GPIO
import time
import picamera
import paho.mqtt.client as mqtt
import pi_switch
from time import sleep

# Access Granted - 100
# Access Denied - 200
# Sorry, Try Again - 300
# Wait, calling owner.. - 400
# Take pic again - 500

res = ""

sender = pi_switch.RCSwitchSender()
sender.enableTransmit(2)

GPIO.setmode(GPIO.BCM)
GPIO.setup(23, GPIO.OUT) 

# connect event
def on_connect(mosq, obj, rc):
    client.subscribe('Result', 0)

# subscription event
def on_subscribe(client, userdata, mid, gqos):
    print('Subscribed: ' + str(mid))

# message event
def on_message(mosq, obj, msg):
    global res
    print(msg.topic + " " + str(msg.qos) + " " + str(msg.payload))
    if msg.topic == 'Result':
      res = str(msg.payload)
      print(res)
    if res == "Access Granted" :
      sender.sendDecimal(100,24)
      GPIO.output(23, 1)
      time.sleep(1)
      GPIO.output(23, 0)
      time.sleep(1)
    elif res == "Access Denied" :
      sender.sendDecimal(200,24)
    elif res == "Sorry, Try Again" :
      sender.sendDecimal(300,24)
    elif res == "Wait, calling owner.." :
      sender.sendDecimal(400,24)
    elif res == "Take pic again" :
      sender.sendDecimal(500,24)
      print "Inside Take Pic"

client = mqtt.Client()
client.on_message = on_message
client.on_connect = on_connect
client.on_subscribe = on_subscribe

# Connect on Mosquitto
client.connect("52.67.104.98", 1883,60)

# Continue the network loop
client.loop_start()
print "Started"

GPIO.setup(18, GPIO.IN, pull_up_down=GPIO.PUD_UP)

try:
    while True:
        input_state = GPIO.input(18)
        if input_state == True:
            
            print('Button Pressed. Taking picture')
            camera = picamera.PiCamera()
            camera.start_preview()
            sleep(1) #Camera needs some time to warm up
            camera.capture('photo1.jpg', resize=(800,800))
            camera.stop_preview()
            camera.close()
            print('Sending photo to cloud')
 
            f = open('photo1.jpg', 'rb')
            fileContent = f.read()
            byteArr = bytearray(fileContent)
            client.publish("camera", byteArr, 0)
            sleep(5000)

finally:
    GPIO.cleanup()
