package org.rozkladbot.entities;

import org.rozkladbot.DBControllers.GroupDB;

public class Group {
    private String groupName;
    private String faculty = "1";
    private String course;
    private String institute;
    private long groupNumber;

    public Group() {

    }

    public Group(String institute, String groupName, String faculty, long groupNumber, String course) {
        this.groupName = groupName;
        this.course = course;
        this.institute = institute;
        this.groupNumber = groupNumber;
        this.faculty = faculty;
    }
    public Group(String groupName, String course, String institute) {
        this.groupName = groupName;
        this.course = course;
        this.institute = institute;
    }
    public Group(String groupName, String course, String faculty, String institute) {
        this.groupName = groupName;
        this.course = course;
        this.institute = institute;
        this.faculty = faculty;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }
    public long getGroupNumber() {
        return groupNumber;
    }
    public static String getGroupNumberAsString(String groupName) {
        return String.valueOf(GroupDB.getGroups().get(groupName).groupNumber);
    }
    public void setGroupNumber(long groupNumber) {
        this.groupNumber = groupNumber;
    }
}
