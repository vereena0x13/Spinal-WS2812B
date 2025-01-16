import sys
import serial
import signal
import time


should_exit = False


def signal_handler(sig, frame):
    global should_exit
    should_exit = True


def main():
    if len(sys.argv) < 2:
        print("usage: %s <uart>" % sys.argv[0])
        sys.exit(1)

    signal.signal(signal.SIGINT, signal_handler)

    dev = serial.Serial(sys.argv[1], 3_000_000)
    # time.sleep(0.5)

    #dev.write(3)
    #dev.flush()
    #print(dev.read(1))
    #dev.write(3)
    #dev.flush()
    #print(dev.read(1))
    #dev.write(0)
    #dev.flush()
    #print(dev.read(1))
    #dev.write(42)
    #dev.flush()
    #print(dev.read(1))
    #dev.write(0)
    #dev.flush()
    #print(dev.read(1))

    for x in range(3):
        for y in range(3):
            dev.write(3 + x)
            dev.flush()
            print(dev.read(1))
    
            dev.write(3 + y)
            dev.flush()
            print(dev.read(1))
            
            dev.write(42)
            dev.flush()
            print(dev.read(1))
            
            dev.write(11)
            dev.flush()
            print(dev.read(1))
            
            dev.write(0)
            dev.flush()
            print(dev.read(1))
        
            print()
            
    
    dev.close()



if __name__ == "__main__":
    main()