# BOLT (Android)

WARNING : This code is not finished and/or fully functionnal!

## The project

This repository contains the Android code for the BOLT project.

BOLT is a machine that tests your reflex speed. The machine has buttons which contain embedded LEDs. When an LED turns on, the user must press the matching button. The machine then calculates the user's reflexes based on the time it took the user to press the button.

The project has two components.
1. The arduino that controls the buttons and LED's. ([Code here](https://github.com/SUPERETDUPER/bolt-arduino)).
2. An android phone which communicates with the Arduino and displays the results. (this repository)

## The code

If complete, the Android app will be able to control the game by deciding when to turn on and off the LEDs. It will then display the average reaction time speed for the user.

### Bluetooth

To communicate via bluetooth the Arduino and the Android phone send each other packets. The packet's format is defined in the `bluetooth-protocol.md` file of the Arduino repository, [here](https://github.com/SUPERETDUPER/bolt-arduino).
