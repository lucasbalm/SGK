import RPi.GPIO as GPIO
import time
import picamera
import paho.mqtt.client as mqtt
import pi_switch
import pygame
from time import sleep

# Access Granted - 100 - Foto reconhecida com acc > 60%
# Access Denied - 200 - 
# Sorry, Try Again - 300
# Wait, calling owner.. - 400
# Take pic again - 500

res = ""
ringflag = True #True = tocar campainha / False = não tocar campainha

sender = pi_switch.RCSwitchSender()
sender.enableTransmit(2)

GPIO.setmode(GPIO.BCM)
GPIO.setup(23, GPIO.OUT)
pygame.mixer.init()

def playsound(sound, waitsound):
    pygame.mixer.music.load(sound)
    pygame.mixer.music.set_endevent(pygame.constants.USEREVENT)
    pygame.mixer.music.play()
    while pygame.mixer.music.get_busy() == waitsound:
        continue

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
        ringflag = True
        playsound("acess_granted.wav",True)
    elif res == "Access Denied" :
        sender.sendDecimal(200,24)
        ringflag = True
        playsound("acess_denied.wav",True)
    elif res == "Sorry, Try Again" :
        sender.sendDecimal(300,24)
        playsound("norecog.wav",True)
    elif res == "Wait, calling owner.." :
        sender.sendDecimal(400,24)
        playsound("calling_owner.wav",True)
    elif res == "Take pic again" :
        sender.sendDecimal(500,24)
        print "Tire a foto novamente"
        playsound("takepic_again.wav",True)

client = mqtt.Client()
client.on_message = on_message
client.on_connect = on_connect
client.on_subscribe = on_subscribe

# Connect on Mosquitto
client.connect("52.67.104.98", 1883,60)

# Continue the network loop
client.loop_start()
print "Iniciado"

GPIO.setup(18, GPIO.IN, pull_up_down=GPIO.PUD_UP)

try:
    while True:
        input_state = GPIO.input(18)
        if input_state == True:
            if(ringflag):
                playsound("campainha.wav", False)
                ringflag = False
            print('Botão pressionado. Tirando foto ...')
            camera = picamera.PiCamera()
            camera.start_preview()
            sleep(1) #Camera needs some time to warm up
            camera.capture('photo1.jpg', resize=(800,800))
            camera.stop_preview()
            camera.close()
            print('Enviando foto ...')
 
            f = open('photo1.jpg', 'rb')
            fileContent = f.read()
            byteArr = bytearray(fileContent)
            client.publish("camera", byteArr, 0)
            sleep(5000) #Para teste sem botão

finally:
    GPIO.cleanup()
