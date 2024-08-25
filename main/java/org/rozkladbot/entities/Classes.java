package org.rozkladbot.entities;

public class Classes {
    int pairNumber;
    String pairTime;
    String pairDetails;
    String subject;

    public Classes(int pairNumber, String pairTime, String pairDetails, String subject) {
        this.pairNumber = pairNumber;
        this.pairTime = pairTime;
        this.pairDetails = pairDetails;
        this.subject = subject;
    }

    public Classes() {
    }

    public int getPairNumber() {
        return pairNumber;
    }

    public void setPairNumber(int pairNumber) {
        this.pairNumber = pairNumber;
    }

    public String getPairTime() {
        return pairTime;
    }

    public void setPairTime(String pairTime) {
        this.pairTime = pairTime;
    }

    public String getPairDetails() {
        return pairDetails;
    }

    public void setPairDetails(String pairDetails) {
        this.pairDetails = pairDetails;
    }

    @Override
    public String toString() {
        return "%d. %s - %s\n".formatted(pairNumber, pairTime, pairDetails);
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}