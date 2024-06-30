package org.rozkladbot.entities;

public class Group {
    private String group;
    private String faculty = "1";
    private String course;
    private String institute;
    private long groupNumber;

    public Group() {

    }

    public Group(String institute, String group, String faculty, long groupNumber, String course) {
        this.group = group;
        this.course = course;
        this.institute = institute;
        this.groupNumber = groupNumber;
    }
    public Group(String group, String course, String institute) {
        this.group = group;
        this.course = course;
        this.institute = institute;
    }
    public Group(String group, String course, String faculty, String institute) {
        this.group = group;
        this.course = course;
        this.institute = institute;
        this.faculty = faculty;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
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

    public void setGroupNumber(long groupNumber) {
        this.groupNumber = groupNumber;
    }
}
