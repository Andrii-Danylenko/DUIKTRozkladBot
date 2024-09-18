package org.rozkladbot.entities;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import org.rozkladbot.DBControllers.GroupDB;

@Entity
@Table(name = "groups")
public class Group {
    @Id
    @Column(name = "groupNumber")
    private long groupNumber;
    @Column(name = "groupName", nullable = false, unique = true)
    private String groupName;
    @Column(name = "faculty", nullable = false)
    private int faculty;
    @Column(name = "course", nullable = false)
    private int course;
    @Column(name = "institute", nullable = false)
    private String institute;

    public Group() {

    }

    public Group(String institute, String groupName, int faculty, long groupNumber, int course) {
        this.groupName = groupName;
        this.course = course;
        this.institute = institute;
        this.groupNumber = groupNumber;
        this.faculty = faculty;
    }
    public Group(String groupName, int course, String institute) {
        this.groupName = groupName;
        this.course = course;
        this.institute = institute;
    }
    public Group(String groupName, int course, int faculty, String institute) {
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

    public int getFaculty() {
        return faculty;
    }

    public void setFaculty(int faculty) {
        this.faculty = faculty;
    }

    public int getCourse() {
        return course;
    }

    public void setCourse(int course) {
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

    @Override
    public String toString() {
        return "Group{" +
               "  groupNumber=" + groupNumber +
               ", groupName='" + groupName + '\'' +
               ", faculty='" + faculty + '\'' +
               ", course='" + course + '\'' +
               ", institute='" + institute + '\n' +
               '}';
    }
}
