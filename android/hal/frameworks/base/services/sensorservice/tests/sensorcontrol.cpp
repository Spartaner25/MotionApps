/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <android/sensor.h>
#include <gui/Sensor.h>
#include <gui/SensorManager.h>
#include <gui/SensorEventQueue.h>
#include <utils/Looper.h>

#include <binder/IServiceManager.h>
#include <gui/ISensorServer.h>
#include <gui/ISensorEventConnection.h>
#include <gui/Sensor.h>
#include <gui/SensorManager.h>
#include <gui/SensorEventQueue.h>

#include <utils/Vector.h>

//#define DEF_RESTRICT_VALID_RATES

using namespace android;

/*
    Typedefs
*/
typedef unsigned long msecs_t;

/*
    Global variables
*/

static const bool mplOnly = false;
static bool printOnScreen = false;
static bool verbose = false;

static int numSensors = 0;
static nsecs_t sStartTime = 0;
static int32_t sensorMask = 0;
static const int nReqInputPars = 1;
#ifdef DEF_RESTRICT_VALID_RATES
static const int validRates[] = {5, 16, 50, 100, 200};
#else
static const int validRateRange[] = {5, 1000};
#endif
static int id2index[50]; // all elements must be initialized to -1

// the list of sensors 
static Vector<Sensor const*> sensorList;
static nsecs_t *timestampList;

/*
    Functions
*/

/**
 *  Get current timestamp in nanoseconds
 */
nsecs_t now_ns(void)
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (nsecs_t) ts.tv_sec * 1000000000 + ts.tv_nsec;
}

msecs_t now_ms(void)
{
    return (msecs_t)(now_ns() / 1000000LL);
}

/**
 *  Lookup array index based on the info in the ASensorEvent
 */
int32_t build_sensor_id(int32_t sensor, int32_t type)
{
    if (sensor > (1L << 20))
        return (type + 20);
    else
        return sensor;
}

/**
 *  Callback to process any incoming sensor data
 */
int receiver(int fd, int events, void* data)
{
    SensorManager& mgr(SensorManager::getInstance());
    sp<SensorEventQueue> q((SensorEventQueue*)data);
    ssize_t n;
    ASensorEvent buffer[8];

    while ((n = q->read(buffer, 8)) > 0) {
        for (int i = 0; i < n; i++) {

            /*
                ASensorEvent class definition can be found in
                   'frameworks/base/native/include/android/sensor.h',
                whose definition should match the one for:
                   'hardware/libhardware/include/hardware/sensor.h'.
            */
            const int32_t sensorId = build_sensor_id(buffer[i].sensor, buffer[i].type);
            const int sensorIndex = id2index[sensorId];
            if (sensorIndex == -1) {
                printf("Error : unrecognized sensor event with "
                       "sensor=%d, type=%d\n",
                       buffer[i].sensor, buffer[i].type);
                return -1;
            }

            float deltaT = float(
                buffer[i].timestamp - timestampList[sensorIndex]) / 1000000.f;
            timestampList[sensorIndex] = buffer[i].timestamp;

            // accuracy, aka status
            // NOTE: 
            //  accuracy not available for Rotation Vector sensors (type = 11)
            //  (this is a possible maintenance nightmare: if the type # ever
            //   changes, this will break)
            int accuracy;
            if (buffer[i].type == 11)
                accuracy = -1;
            else
                accuracy = buffer[i].vector.status;

            if (printOnScreen)
                printf("DATALOG "
                       "%-30s, "                    // sensor name
                       "%+13f, %+13f, %+13f, "      // data, 3 values
                       "%lld, "                     // timestamp
                       "%.3f, "                     // delta timestamp
                       "%d, "                       // accuracy
                       "\n",
                       sensorList[sensorIndex]->getName().string(),
                       buffer[i].vector.v[0],
                       buffer[i].vector.v[1],
                       buffer[i].vector.v[2],
                       buffer[i].timestamp,
                       deltaT,
                       accuracy
                );
        }
    }
    if (n < 0 && n != -EAGAIN) {
        printf("error reading events (%s)\n", strerror(-n));
    }
    return 1;
}

/**
 *  help
 */
void print_help(char *argv0)
{
    printf(
        "\n"
        "Usage : %s [OPTIONs] <sensor1,rate1> [<sensor2,rate2> ...]\n"
        "\n"
        "        where \n"
        "           <sensorN,rateN>\n"
        "                       is the comma separated sensor index or name\n"
        "                       and required rate of the sensor in Hz.\n"
        "                       Supported rates are > 5 Hz.\n"
        "                       Run with -h/--help for the reference list of\n"
        "                       sensor available and their associated index.\n"
        "\n"
        "        Options\n"
        "           -h / --help\n"
        "                       show this help screen and exit.\n"
        "           -p / --print\n"
        "                       print sensor data on screen.\n"
        "           -t / --time TIME\n"
        "                       run for the provided amount of milliseconds.\n"
        "           -v / --verbose\n"
        "                       more verbose output (exclusing sensor that is\n"
        "                       enabled separately with option -p/--print.\n"
        "\n",
        argv0);
}

/**
 *  Main loop
 */
int main(int argc, char** argv)
{
    SensorManager& mgr(SensorManager::getInstance());
    bool enable = false;
    msecs_t duration = 0L;

    // parse command-line arguments
    if (argc < (nReqInputPars + 1)) {
        printf(
            "\n"
            "Error : invalid number of command-line arguments.\n"
            "        You must specify at at least %d parameters.\n",
            nReqInputPars);
        print_help(argv[0]);
        return(10);
    }

    // initilize lookup sensor id -> array index lookup
    for (int ii = 0; ii < int(sizeof(id2index) / sizeof(id2index[0])); 
         ii++) {
        id2index[ii] = -1;
    }

    // load the list of available sensors in the manager
    //  used to validate the sensor's name received as input
    Sensor const* const* list;
    ssize_t count = mgr.getSensorList(&list);
    printf("\n");
    printf("Sensors' list:\n");
    for (int ii = 0; ii < int(count); ii++) {
        const Sensor *sensor = list[ii];
        if (mplOnly) {
            if (strncmp(sensor->getName().string(), "MPL", 3)) {
                continue;
            }
        }

        // sensor id,
        //  if an MPL sensor, use 'sensor' field of the event to index the 
        //      event to the corresponding element in the array of sensors.
        //      The 'sensor' field in the ASensorEvent matches the index of the
        //      sensor in Sensors' list -1.
        //  if a Google sensor, the 'sensor' field is unitialized. Use the 20 +
        //      'type' field instead to identify the sensor the event 
        //      corresponds to. Hopefully Google will never release an AOSP with
        //      sensor using duplicated 'type' field.
        int sensorId;
        if (!strncmp(sensor->getName().string(), "MPL", 3)) {
            sensorId = ii;
        } else {
            sensorId = sensor->getType() + 20;
        }

        if (sensorId > int(sizeof(id2index) / sizeof(id2index[0]))) {
            printf("\n");
            printf("Error : ID (%d) exceeds %d - "
                   "please extend id2index's array size.\n",
                   sensorId, sizeof(id2index) / sizeof(id2index[0]));
            printf("\n");
            return(20);
        }

        // the sensor id is used to lookup in the id2index map
        id2index[sensorId] = sensorList.add(sensor);
        printf("\t%-2d -> type %2d/%3d, name '%s'\n", 
               ii + 1, sensor->getType(), sensorId,
               sensor->getName().string());
    }
    printf("\n");
    numSensors = sensorList.size();

    // create the list of last timestamp on the fly based on the sensor list
    timestampList = new nsecs_t[numSensors];
    for (int ii = 0; ii < numSensors; ii++) {
        timestampList[ii] = now_ns();
    }

    //
    // validate the command-line arguments: OPTIONS
    //
    int lastDashOption = 1;
    for (int ii = 1; ii < argc; ii++) {
        if(strcmp(argv[ii], "-h") == 0 ||
           strcmp(argv[ii], "--help") == 0) {
            print_help(argv[0]);
            return 0;

        } else if(strcmp(argv[ii], "-t") == 0 || 
                  strcmp(argv[ii], "--time") == 0) {
            duration = strtoul(argv[++ii], NULL, 10);
            if (duration == 0) {
                printf(
                    "\n"
                    "Error : invalid time specification '%s'.\n"
                    "        Please specify an integer number > 0 for the\n"
                    "        number of milliseconds.\n"
                    "\n",
                    argv[ii]);
            }
            printf("-- polling sensors for %lu seconds\n", 
                   duration / 1000L);

        } else if(strcmp(argv[ii], "-p") == 0 || 
                  strcmp(argv[ii], "--print") == 0) {
            printf("-- enabled printing on screen\n");
            printOnScreen = true;

        } else if(strcmp(argv[ii], "-v") == 0 || 
                  strcmp(argv[ii], "--verbose") == 0) {
            printf("-- enabled verbose output\n");
            verbose = true;

        } else {
            /* break at the first not 'dash' command-line parameter found:
               the rest will be expected to be strings of sensor,rate 
               selections. 
               Save */
            lastDashOption = ii - 1;
            break;
        }
    }

    if (lastDashOption == argc - 1) {
        // user did not provide a sensor selection, bail out
        printf(
            "\n"
            "Error : you must specify at least one sensor/rate selection\n"
            "\n");
        print_help(argv[0]);
        return (40);
    }

    // acquire a smart-pointer reference to the event queue: as long 
    //  as this object is not released/destroyed the sensor enabled will keep 
    //  staying enabled.
    sp<SensorEventQueue> q = mgr.createEventQueue();
    if (verbose)
        printf("queue = %p\n", q.get());

    for (int ii = lastDashOption + 1; ii < argc; ii++) {

        char *clSensorSel = strtok(argv[ii], ",");
        char *clRateSel = strtok(NULL, " ");
        if (verbose)
            printf("sensor = '%s', rate = %s\n", clSensorSel, clRateSel);

        //
        // validate the command-line arguments: sensor_name
        //
        String8 sensorSel(clSensorSel);
        int sensorIndex = -1;
        // -> check if it is a sensor index (Warning: system dependent)
        int tmpIndex = atoi(sensorSel.string());
        if (tmpIndex > 0) {
            if (tmpIndex <= int(sensorList.size())) {
                sensorIndex = tmpIndex;
            } else {
                printf("\n");
                printf("Error : cannot find requested sensor index '%s'\n",
                       sensorSel.string());
                printf("\n");
                return(20);
            }
        } else {
            // -> check if it is the actual sensor name
            for (int ii = 0; ii < int(sensorList.size()); ii++) {
                if (sensorSel == sensorList[ii]->getName()) {
                    sensorIndex = ii + 1;
                }
            }
            if (sensorIndex == -1) {
                printf("\n");
                printf("Error : cannot find requested sensor name '%s'\n",
                       sensorSel.string());
                printf("\n");
                return(21);
            }
        }
        sensorIndex--;
        Sensor const* sensor = sensorList[sensorIndex];
        if (verbose)
            printf("sensor name/index = %s / %d\n", 
                   sensor->getName().string(), sensorIndex + 1);

        //
        // validate the command-line arguments: rate
        //
        String8 rateLiteral(clRateSel);
        int rateHz = atoi(rateLiteral.string());
        if (rateHz == 0) {
            printf("\n");
            printf("Error : invalid rate specification '%s'\n", 
                   rateLiteral.string());
            printf("\n");
            return(30);
        }
#ifdef DEF_RESTRICT_VALID_RATES
        bool valid = false;
        for (int ii = 0; 
             ii < int(sizeof(validRates) / sizeof(validRates[0])); ii++) {
            if (rateHz == validRates[ii]) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            printf("\n");
            printf("Error : invalid rate specification '%d'\n", rateHz);
            printf("        Only %d, %d, %d, and %d Hz supported\n",
                   validRates[0], validRates[1], validRates[2], validRates[3]);
            printf("\n");
            return(31);
        }
#else
        if (rateHz < validRateRange[0] || rateHz > validRateRange[1]) {
            printf("\n");
            printf("Error : invalid rate specification '%d'\n", rateHz);
            printf("        Only rate in the [%d, %d] Hz range are allowed\n",
                   validRateRange[0], validRateRange[1]);
            printf("\n");
            return(31);
        }
#endif
        if (verbose)
            printf("rate selection = %d Hz\n", rateHz);

        //
        // enable the sensor
        //
        nsecs_t delayNs = nsecs_t(1000000000LL / rateHz);
        printf("## enabling '%s' @ %d Hz (%lld ms)\n", 
               sensor->getName().string(), rateHz, delayNs / 1000000LL);
        q->enableSensor(sensor);
        q->setEventRate(sensor, delayNs);
    }

    //
    // process the sensor data
    //
    msecs_t durationStart = (long)(now_ns() / 1000000LL);
    msecs_t durationKa = durationStart;
    msecs_t nowMs = 0L;
    sp<Looper> loop = new Looper(false);
    loop->addFd(q->getFd(), 0, ALOOPER_EVENT_INPUT, receiver, q.get());
    /* go until interrupted (SIGKILL/CTRL+c) or time has expired */
    while (duration == 0 ||
           ((nowMs = now_ms()) - durationStart) < duration) {
        int32_t ret = loop->pollOnce(-1);
        switch (ret) {
        case ALOOPER_POLL_WAKE:
            break;
        case ALOOPER_POLL_CALLBACK:
            break;
        case ALOOPER_POLL_TIMEOUT:
            break;
        case ALOOPER_POLL_ERROR:
            break;
        default:
            printf("ugh? poll returned %d\n", ret);
            break;
        }
        if (duration > 0 && !printOnScreen) {
            if ((nowMs - durationKa) > 1000L) { // every 1 second
                printf("time elapsed: %lu ms\n", nowMs - durationStart);
                durationKa = nowMs;
            }
        }
    }

    return 0;
}
