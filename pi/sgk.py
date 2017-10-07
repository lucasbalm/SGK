#!/usr/bin/python
# -*- coding: utf-8 -*-
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

sender = pi_switch.RCSwitchSender()
sender.enableTransmit(2)

deb = False

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
        playsound("/home/pi/SGK/pi/acess_granted.wav",True)
        time.sleep(5)
        GPIO.output(23, 0)        
        time.sleep(5)
        deb = False        
    elif res == "Access Denied" :
        sender.sendDecimal(200,24)
        playsound("/home/pi/SGK/pi/acess_denied.wav",True)
        time.sleep(5)
        deb = False 
    elif res == "Sorry, Try Again" :
        sender.sendDecimal(300,24)
        playsound("/home/pi/SGK/pi/norecog.wav",True)
        time.sleep(5)
        deb = False 
    elif res == "Wait, calling owner.." :
        sender.sendDecimal(400,24)
        playsound("/home/pi/SGK/pi/calling_owner.wav",True)
        time.sleep(5)
        deb = False 
    elif res == "Take pic again" :
        sender.sendDecimal(500,24)
        print "Tire a foto novamente"
        playsound("/home/pi/SGK/pi/takepic_again.wav",True)
        time.sleep(5)
        deb = False 
    elif res == "Picture" :
        print "Tirando foto da porta"
        camera = picamera.PiCamera()
        camera.start_preview()
        sleep(1) #Camera needs some time to warm up
        camera.capture('/home/pi/SGK/pi/photo2.jpg', resize=(800,800))
        camera.stop_preview()
        camera.close()
        print('Enviando foto ...')
        f = open('/home/pi/SGK/pi/photo2.jpg', 'rb')
        fileContent = f.read()
        byteArr = bytearray(fileContent)
        client.publish("picture", byteArr, 0)
        print('Foto enviada')
        time.sleep(5)
        deb = False 
        

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
        if deb == False:
            if input_state == False:
                deb = True
                playsound("/home/pi/SGK/pi/ringbell.wav", False)
                print('Botão pressionado. Tirando foto ...')
                camera = picamera.PiCamera()
                camera.start_preview()
                sleep(1) #Camera needs some time to warm up
                camera.capture('/home/pi/SGK/pi/photo1.jpg', resize=(800,800))
                camera.stop_preview()
                camera.close()
                print('Enviando foto ...')
     
                f = open('/home/pi/SGK/pi/photo1.jpg', 'rb')
                fileContent = f.read()
                byteArr = bytearray(fileContent)
                client.publish("camera", byteArr, 0)
finally:
    GPIO.cleanup()
