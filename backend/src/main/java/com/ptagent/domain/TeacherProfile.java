package com.ptagent.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TeacherProfile {
    public long teacherId;
    public String realName;
    public int gender;
    public String education;
    public String graduateSchool;
    public boolean is985;
    public boolean is211;
    public boolean isKeyUniversity;
    public List<String> subjects = new ArrayList<>();
    public String teachingExperience;
    public String expectedRate;
    public List<String> availableTime = new ArrayList<>();
    public List<String> serviceArea = new ArrayList<>();
    public String personalIntro;
    public double avgRating = 5.0;
    public String contactPhone;
    public String contactWechat;
    public boolean hasTeacherCert;
    public boolean normalUniversity;
    public boolean competitionExperience;

    public List<String> tags() {
        List<String> tags = new ArrayList<>();
        if (is985) {
            tags.add("985");
        }
        if (is211) {
            tags.add("211");
        }
        if (isKeyUniversity) {
            tags.add("重本");
        }
        if (normalUniversity) {
            tags.add("师范类");
        }
        if (competitionExperience) {
            tags.add("有竞赛经验");
        }
        if (hasTeacherCert) {
            tags.add("有教师资格证");
        }
        return tags;
    }

    public Map<String, Object> toMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("teacherId", teacherId);
        map.put("displayName", user == null ? "" : user.displayName);
        map.put("enabled", user == null || user.enabled);
        map.put("realName", realName);
        map.put("gender", gender);
        map.put("genderLabel", gender == 1 ? "男" : "女");
        map.put("education", education);
        map.put("graduateSchool", graduateSchool);
        map.put("is985", is985);
        map.put("is211", is211);
        map.put("isKeyUniversity", isKeyUniversity);
        map.put("subjects", subjects);
        map.put("teachingExperience", teachingExperience);
        map.put("expectedRate", expectedRate);
        map.put("availableTime", availableTime);
        map.put("serviceArea", serviceArea);
        map.put("personalIntro", personalIntro);
        map.put("avgRating", avgRating);
        map.put("contactPhone", contactPhone);
        map.put("contactWechat", contactWechat);
        map.put("hasTeacherCert", hasTeacherCert);
        map.put("normalUniversity", normalUniversity);
        map.put("competitionExperience", competitionExperience);
        map.put("tags", tags());
        return map;
    }
}
