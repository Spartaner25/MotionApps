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

using namespace android;

/*
    Class definition
*/
class SensorStats {
public:
    bool enabled;
    int rateIndex;
private:
    Sensor const* sensor;
    // stats
    static const unsigned int window = 1000;
    Vector<float> cpu_utime;
    Vector<float> cpu_stime;
    long timeMs;
    long samples;

public:
    SensorStats() {
        sensor = NULL;
        enabled = false;
        rateIndex = 3;
        cpu_utime.clear();
        cpu_stime.clear();
        timeMs = 0L;
        samples = 0L;
    }
    SensorStats(Sensor const* s) {
        sensor = s;
        enabled = false;
        rateIndex = 3;
        cpu_utime.clear();
        cpu_stime.clear();
        timeMs = 0L;
        samples = 0L;
    }

    // getters
    Sensor const* getSensor() { return sensor; }
    float getAvgCpuUtime() { 
        float avg = 0.f;
        for (unsigned int ii = 0; ii < cpu_utime.size(); ii++) {
            avg += cpu_utime[ii];
        }
        return (avg / cpu_utime.size());
    }
    float getAvgCpuStime() {
        float avg = 0.f;
        for (unsigned int ii = 0; ii < cpu_stime.size(); ii++) {
            avg += cpu_stime[ii];
        }
        return (avg / cpu_stime.size());
    }
    long getTime() { return timeMs; }
    long getSamples() { return samples; }

    // setters
    void addCpuUtime(float utime) {
        if (cpu_utime.size() >= window) {
            cpu_utime.removeAt(0);
        }
        cpu_utime.insertAt(utime, cpu_utime.size());
    }
    void addCpuStime(float stime) {
        if (cpu_stime.size() >= window) {
            cpu_stime.removeAt(0);
        }
        cpu_stime.insertAt(stime, cpu_stime.size());
    }
    void addSample() {
        samples++;
    }
    void addTime(long more) {
        timeMs += more;
    }
};

/*
    Global variables
*/

static int numSensors = 0;
static nsecs_t sStartTime = 0;
static int32_t sensorMask = 0;
static int print_on_screen = 0;

/*
    Functions
*/

/**
 *  Get current timestamp in nanoseconds
 */
int64_t now_ns(void)
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t) ts.tv_sec * 1000000000 + ts.tv_nsec;
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

    static nsecs_t oldTimeStamp = 0;

    while ((n = q->read(buffer, 8)) > 0) {
        for (int i = 0; i < n; i++) {
            float t;
            if (oldTimeStamp) {
                t = float(buffer[i].timestamp - oldTimeStamp) / s2ns(1);
            } else {
                t = float(buffer[i].timestamp - sStartTime) / s2ns(1);
            }
            oldTimeStamp = buffer[i].timestamp;
            
            if (print_on_screen)
                printf("%-20s data : %+10f, %+10f, %+10f timestamp:%lld\n",
                       mgr.getDefaultSensor(buffer[i].type)->getName().string(),
                       buffer[i].data[0], 
                       buffer[i].data[1], 
                       buffer[i].data[2],
                       buffer[i].timestamp);
        }
    }
    if (n < 0 && n != -EAGAIN) {
        printf("error reading events (%s)\n", strerror(-n));
    }
    return 1;
}

/**
 *  Grep output of ps command using 'busybox pgrep'
 *  to find the 'system_server' process id
 */
int get_pid_of_system_server(void)
{
    char psBuffer[128]; 
    FILE *iopipe; 
    int pid = 0;
    
    if((iopipe = popen("busybox pgrep system_server", "r")) == NULL)
        exit(1);

    //ampl is the command (executable) whose pid you want to find 
    while(!feof(iopipe)) {
        if(fgets(psBuffer, 128, iopipe) != NULL) 
            //puts(psBuffer); 
            // or use atoi(psBuffer) to get numerical value 
            pid = atoi(psBuffer);
    } 
    //printf("\nProcess returned %d\n", pclose(iopipe));

    return pid;
}

/**
 *  Gather the CPU utime and stime reading /proc/stat
 *  and /proc/PID/stat.
 */
void cpu_stats(char *ss_stat_file, int *time_total, int *utime, int *stime)
{
    char output[1000];
    FILE *stat_file;
    int pos;
    int i;
    char *loc;

    //# Example content of /proc/stat
    //> cpu  192369 7119 480152 122044337 14142 9937 26747 0 0
    
    stat_file = fopen("/proc/stat", "r");
    fscanf(stat_file, "%[^\n]", output);
    fclose(stat_file);
    
    pos = 10;
    i = 0;
    loc = strtok(output, " ");
    *time_total = 0;
    while (i++ < pos) {
        loc = strtok(NULL, " "); 
        *time_total += atoi(loc);
    }

    //# Example content of /proc/PID/stat
    //# 0     1               2 3     4     5 6  7 8       9     10111213   14
    //> 13129 (system_server) S 13099 13099 0 0 -1 4194624 24513 0 2 0 1549 6114 0 0 20 0 66 0 6503632 550862848 140

    stat_file = fopen(ss_stat_file, "r");
    fscanf(stat_file, "%[^\n]", output);
    fclose(stat_file);

    pos = 13;
    i = 0;
    loc = strtok(output, " ");
    while (i++ < pos) {
        loc = strtok(NULL, " "); 
    }
    *utime = atoi(loc);

    loc = strtok(NULL, " "); 
    *stime = atoi(loc);

    return;
}

int main(int argc, char** argv)
{
    int time_total_before = 0, utime_before = 0, stime_before = 0;
    float cpu_utime_before = 0.f, cpu_stime_before = 0.f;
    char system_server_stat_file[100];
    SensorManager& mgr(SensorManager::getInstance());
    Vector<Sensor const*> sensorList;
    Vector<SensorStats*> sensorStatsList;

    // explore the list of available sensors in the manager
    //  and load them in the vector
    Sensor const* const* list;
    ssize_t count = mgr.getSensorList(&list);
    printf("Sensors' list:\n");
    for (int ii = 0; ii < int(count); ii++) {
        const Sensor *sensor = list[ii];
        const bool mplOnly = false;
        if (mplOnly) {
            if (strncmp(sensor->getName().string(), "MPL", 3)) {
                continue;
            }
        }
        SensorStats *ss = new SensorStats(sensor);
        sensorList.add(sensor);
        sensorStatsList.add(ss);
        printf("\t%-2d -> %s\n", ii, sensor->getName().string());
    }
    printf("\n");
    numSensors = sensorList.size();

    sp<SensorEventQueue> q = mgr.createEventQueue();
    printf("queue = %p\n", q.get());

    int pid = get_pid_of_system_server();
    printf("system_server's pid = %d\n", pid);
    sprintf(system_server_stat_file, "/proc/%d/stat", pid);

    while (1) {
        int r_index = rand() % numSensors;

        SensorStats *stats = sensorStatsList[r_index];
        Sensor const* sensor = stats->getSensor();

        printf("\n");
        printf("###########################################################\n");
        // note always enable since we disable a sensor at the end of the 
        //  random r_time
        //int r_enable = rand() % 2;
        int r_enable = 1;
        if (r_enable) {
            printf("## enabling \"%s\"\n", sensor->getName().string());
            q->enableSensor(sensor);
            q->setEventRate(sensor, ms2ns(20)); // hardcoded
            sensorMask |= (1 << sensor->getType());
            // quick workaround to clear the statistics - delete and recreate 
            //      SensorStat object
            stats->enabled = true;
            stats->rateIndex = 2; // hardcoded
        } else {
            printf("## disabling \"%s\"\n", sensor->getName().string());
            q->disableSensor(sensor);
            sensorMask &= ~(1 << sensor->getType());
            stats->enabled = false;
        }
        printf("###########################################################\n");
        printf("\n");

        // show all the sensor enabled
        printf("Sensors enabled : ");
        for (int ii = 0; ii < int(sensorStatsList.size()); ii++) {
            if (sensorStatsList[ii]->enabled) {
                printf("%s, ",
                        sensorStatsList[ii]->getSensor()->getName().string());
            }
        }
        printf("\n");

        sStartTime = systemTime();

        sp<Looper> loop = new Looper(false);
        loop->addFd(q->getFd(), 0, ALOOPER_EVENT_INPUT, receiver, q.get());
        //printf("q->getFd() = %d\n", q->getFd());

        int64_t start = now_ns();
        int64_t now;
        int r_time = 10 + rand() % 20;
        //int r_time = 1000;
        int64_t cpu_stat_start = now_ns();
        int64_t cpu_stat_period = 1;
        
        printf("Looping for %d s, mask %ld\n", r_time, (long)sensorMask);
        while (((now = now_ns()) - start) < 1000000000LL * r_time) {
            if (sensorMask) {
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
                stats->addSample();
            }
            
            if ((now - cpu_stat_start) > 1000000000LL * cpu_stat_period) {
                // get new stats
                int time_total_after, utime_after, stime_after;
                cpu_stats(system_server_stat_file, &time_total_after, &utime_after, &stime_after);
                
                // compute user and system utilization
                int time_total = time_total_after - time_total_before;
                float cpu_utime_after;
                float cpu_stime_after;
                if (time_total) {
                    cpu_utime_after = 
                        100.f * (utime_after - utime_before) / time_total;
                    cpu_stime_after = 
                        100.f * (stime_after - stime_before) / time_total;
                    if (1 /*cpu_utime_after != cpu_usage_before*/) {
                        printf("CPU usage : %.2f%% / %.2f%%\n", 
                               cpu_utime_after, cpu_stime_after);
                        cpu_utime_before = cpu_utime_after;
                        cpu_stime_before = cpu_stime_after;
                    }
                    stats->addCpuUtime(cpu_utime_after);
                    stats->addCpuStime(cpu_stime_after);
                }

                // save time_total, utime, and stime
                time_total_before = time_total_after;
                utime_before = utime_after;
                stime_before = stime_after;

                cpu_stat_start = now;
            }
        }

        stats->addTime(1000L * r_time);

        printf("\n");
        printf("Sensors' statistics\n");
        printf("\t      %-30s avg_utime(%%) avg_stime(%%) time(s) samples\n", "");
        for (int ii = 0; ii < int(sensorStatsList.size()); ii++) {
            const Sensor *sensor = sensorStatsList[ii]->getSensor();
            printf("\t%-2d -> %-30s ", ii, sensor->getName().string());
            printf("%12.2f ", sensorStatsList[ii]->getAvgCpuUtime());
            printf("%12.2f ", sensorStatsList[ii]->getAvgCpuStime());
            printf("%7ld ", sensorStatsList[ii]->getTime());
            printf("%7ld ", sensorStatsList[ii]->getSamples());
            printf("\n");
        }
        printf("\n");
        
        // always disable after - it's a limitation, but to be able to 
        //  keep stats of the CPU load for now it's the only simple way
        printf("disabling \"%s\"\n", sensor->getName().string());
        q->disableSensor(sensor);
        sensorMask &= ~(1 << sensor->getType());
        stats->enabled = false;
    }
    return 0;
}
