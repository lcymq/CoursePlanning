package com.example.courseplanningapp.model;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.courseplanningapp.R;
import com.example.courseplanningapp.constants.Constants;
import com.example.courseplanningapp.ui.CourseList;
import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;


/*
    CourseManager class holds all courses_s
    provides a singleton to access the data from
*/
public class CourseManager {
    private ArrayList<Course> courses = new ArrayList<>();
    private ArrayList<Course> filteredCourses = null;
    private ArrayList<String> addedCourseId = new ArrayList<>();

    // Singleton
    private static CourseManager instance;

    // read the course data file
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private CourseManager(Context context, String major, String startYear, String startSemester, int courseCount) throws IOException {
        SharedPreferences sharedPreferences = context.getSharedPreferences("addedCoursePref", Context.MODE_PRIVATE);
        String serializedCourses = sharedPreferences.getString("addedCourse", "");
        String[] coursesData = serializedCourses.split(",");
        String addedC = "";
        for(String courseId : coursesData) {
            if(courseId != null && !courseId.isEmpty()) {
                addedCourseId.add(courseId);
                addedC += courseId + ",";
            }
        }
        System.out.println("coursesData现在有: " + addedC);

        if (major.equals("Computing Science")){
            readCmptData(context, startYear, startSemester, courseCount);
        } else { // major == null
            InputStreamReader isr;
            try {
                isr = new InputStreamReader(
                        context.openFileInput(Constants.SAVE_DATA_FILENAME),
                        StandardCharsets.UTF_8);
            } catch (IOException e){
                isr = new InputStreamReader(
                        context.getResources().openRawResource(R.raw.select_courses),
                        StandardCharsets.UTF_8);
            }

            BufferedReader reader = new BufferedReader(isr);

            // populate the data structure
            courses.clear();
            System.out.println(courses);

            String line;
            String year, semester, subject, courseNumber, title;

            // iteratively read the whole file
            while((line = reader.readLine()) != null) {
                String[] courseInfo = line.split(",");
                System.out.println("line里面是：" + line);
                System.out.println("addedCourseId是" + addedCourseId);
                year = courseInfo[0];
                semester = courseInfo[1];
                subject = courseInfo[2];
                courseNumber = courseInfo[3];
                title = courseInfo[4];
                Course course = new Course(year, semester, subject, courseNumber, title);

                if(addedCourseId.contains(course.getCourseId())){
                    courses.add(course);
                    System.out.println("此时的course是：" + course.toString());
                    System.out.println("此时的courses是：" + courses.toString());
                }

                for(String courseId: addedCourseId){
                    if (course.getCourseId().equals(courseId)){
                        courses.add(course);
                        System.out.println("此时的course是：" + course.toString());
                        System.out.println("此时的courses是：" + courses.toString());
                    }
                }

            }
        }




        Collections.sort(courses);

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void readCmptData(Context context, String startYear, String startSemester, int courseCount) throws IOException {
        InputStreamReader reader;

        reader = new InputStreamReader(context.getResources().openRawResource(R.raw.cmpt), StandardCharsets.UTF_8);

        courses.clear();

        String year, semester = null, subject, courseNumber, title;
        int yearCode, startSemesterCode, semesterCode,i;
        if(startSemester.equals("Spring")){
            startSemesterCode = 0;
            i = -1;
        } else if (startSemester.equals("Summer")){
            startSemesterCode = 1;
            i = -1 + courseCount;
        } else {
            startSemesterCode = 2;
            i = -1 + 2*courseCount;
        }

        CSVReader csvReader = new CSVReader(reader);
        String[] line;
        csvReader.readNext();
        int j = -1;
        while ((line = csvReader.readNext())!=null) {
            i++; j++;
            yearCode = Integer.parseInt(startYear) + i/(3*courseCount);
            year = "" + yearCode;
            semesterCode = startSemesterCode + j/courseCount;
            switch (semesterCode%3){
                case 0:
                    semester = "Spring";
                    break;
                case 1:
                    semester = "Summer";
                    break;
                case 2:
                    semester = "Fall";
                    break;
                default:
                    break;
            }
            subject = line[0];
            courseNumber = line[1];
            title = line[2];
            Course course = new Course(year,semester,subject, courseNumber, title);
            courses.add(course);
            saveCourseInfoToFile(context,course,Constants.SAVE_DATA_FILENAME);
        }
        Collections.sort(courses);
    }

    public void resort() {
        Collections.sort(courses);
    }

    public void addCourse(Course course){
        courses.add(course);
        addedCourseId.add(course.getCourseId());
    }

    public void removeCourse(Course course) {
        courses.remove(course);
        addedCourseId.remove(course.getCourseId());
    }

    public boolean courseIsAdded(String courseId) {
        return addedCourseId.contains(courseId);
    }

    public void saveCourseIdIntoSharedPreference(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("addedCoursePref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        StringBuilder serializedCourses = new StringBuilder();
        for (String courseId : addedCourseId) {
            serializedCourses.append(courseId).append(",");
        }
        editor.putString("addedCourse", serializedCourses.toString());
        editor.commit();
        System.out.println("点击save按钮后，addedCourse状态：" + addedCourseId);
    }

    public void removeCourseIdFromSharedPreference(Context context, String courseIdToRemove) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("addedCoursePref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        StringBuilder serializedCourses = new StringBuilder();
        for (String courseId : addedCourseId) {
            if (courseId.equals(courseIdToRemove)){
                serializedCourses.append("");
            }else{
                serializedCourses.append(courseId);
            }
            editor.putString("addedCourse", serializedCourses.toString());
            editor.commit();
        }
    }


    // save new added course to txt file
    public void saveCourseInfoToFile(Context context, Course course, String filename){
        String filePath = "/sdcard/Gyt/";
        String fileName = Constants.SAVE_DATA_FILENAME;
        FileOutputStream fileOutputStream = null;
        BufferedWriter bufferedWriter;
        StringBuilder serializedCourses = new StringBuilder();
        try {
            // MODE_APPEND - if a txt file with the same name, then append this file
            fileOutputStream = context.openFileOutput(fileName, Context.MODE_APPEND);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            serializedCourses.append(course.getYear());
            serializedCourses.append(",");
            serializedCourses.append(course.getSemester());
            serializedCourses.append(",");
            serializedCourses.append(course.getSubject());
            serializedCourses.append(",");
            serializedCourses.append(course.getCourseNumber());
            serializedCourses.append(",");
            serializedCourses.append(course.getTitle());
            serializedCourses.append(",");
            serializedCourses.append(course.getVisible());
            serializedCourses.append("\n");
            String data = serializedCourses.toString();
            fileOutputStream.write(data.getBytes());
            System.out.println("写入成功，内容为："+ data);
        }catch (IOException e){
            Toast.makeText(context, "Sorry, there's an error on saving this data", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // read data from txt file
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void loadCourseInfoFromFile(Context context) {
        FileInputStream fileInputStream;
        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            fileInputStream = context.openFileInput(Constants.SAVE_DATA_FILENAME);
            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
            String result = "";
            while((result = bufferedReader.readLine()) != null) {
                stringBuilder.append(result);
            }
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        courses.clear();
        String[] courseInformation;
        String year, semester, subject, courseNumber, title;
        String[] allCourseInfo = stringBuilder.toString().split("\n");
        if (stringBuilder != null && !stringBuilder.toString().isEmpty()) {
            for (String courseInfo : allCourseInfo) {
                System.out.println("courseInfo里面是：" + courseInfo);
                courseInformation = courseInfo.split(",");
                System.out.println("courseInformation里面是：" + courseInformation[0]);
                year = courseInformation[0];
                semester = courseInformation[1];
                subject = courseInformation[2];
                courseNumber = courseInformation[3];
                title = courseInformation[4];
                Course course = new Course(year, semester, subject, courseNumber, title);
                courses.add(course);
                System.out.println("此时的course是：" + course.toString());
                System.out.println("此时的courses是：" + courses.toString());
            }
        }
    }


    public ArrayList<Course> getFilteredCourses() {
        return filteredCourses == null ? courses : filteredCourses;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static CourseManager getInstance(Context context, String major, String startYear, String startSemester, int courseCount) {
        if (instance == null) {
            try {
                instance = new CourseManager(context, major, startYear, startSemester, courseCount);
            } catch (IOException e) {
                Toast.makeText(context, "Sorry, there was an error reading course data", Toast.LENGTH_SHORT).show();
            }
        }
        return instance;
    }


}
