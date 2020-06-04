package com.niallmoran.runsync.models;

import com.niallmoran.runsync.utilities.TimeUtils;

import java.util.Date;

public class ExerciseRecord {

    static final int EXERCISE_TYPE_WALKING = 1001;
    static final int EXERCISE_TYPE_RUNNING = 1002;

    public ExerciseRecord(int duration, long startTime, int exerciseType, long updateTime,
                          long createTime, float maxSpeed, float distance, float calories,
                          float timeOffset, String deviceid, float avgSpeed,
                          long endTime, String uniqueId)
    {
        this.duration = duration;
        this.startTimeTicks = startTime;
        this.exerciseType = exerciseType;
        this.updateTimeTicks = updateTime;
        this.createTimeTicks = createTime;
        this.maxSpeed = maxSpeed;
        this.distance = distance;
        this.calories = calories;
        this.timeOffset = timeOffset;
        this.deviceid = deviceid;
        this.avgSpeed = avgSpeed;
        this.endTimeTicks = endTime;
        this.uniqueId = uniqueId;
    }

    private int duration;
    private int exerciseType;
    private Float maxSpeed;
    private float distance;
    private float calories;
    private float timeOffset;
    private String deviceid;
    private float avgSpeed;

    private long endTimeTicks;
    private long startTimeTicks;
    private long updateTimeTicks;
    private long createTimeTicks;

    private String uniqueId;

    public int getDuration() {
        return duration;
    }

    public Date getStartTime() {
       return TimeUtils.getDatefromTicks(startTimeTicks);
    }

    public int getExerciseType() {
        return exerciseType;
    }

    public Date getUpdateTime() {
        return TimeUtils.getDatefromTicks(updateTimeTicks);
    }

    public String getUpdateTimeText() {
        return TimeUtils.getDateStringfromTicks(updateTimeTicks);
    }

    public String getExerciseTypeText()
    {
        if( this.exerciseType == EXERCISE_TYPE_WALKING )
            return "Walking";
        else
            return "Running";
    }

    public String getEndTimeText() {
        return TimeUtils.getDateStringfromTicks(endTimeTicks);
    }

    public String getStartTimeText() {
        return TimeUtils.getDateStringfromTicks(startTimeTicks);
    }

    public Date getCreateTime() {
        return TimeUtils.getDatefromTicks( createTimeTicks);
    }

    public String getCreateTimeText() {
        return TimeUtils.getDateStringfromTicks(createTimeTicks);
    }

    public Float getMaxSpeed() {
        return maxSpeed;
    }

    public float getDistance() {
        return distance;
    }

    public float getCalories() {
        return calories;
    }

    public float getTimeOffset() {
        return timeOffset;
    }

    public String getDeviceid() {
        return deviceid;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public Date getEndTime() {
        return TimeUtils.getDatefromTicks(endTimeTicks);
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public long getEndTimeTicks() {
        return endTimeTicks;
    }

    public long getStartTimeTicks() {
        return startTimeTicks;
    }

    public long getUpdateTimeTicks() {
        return updateTimeTicks;
    }

    public long getCreateTimeTicks() {
        return createTimeTicks;
    }
}
