package com.example.courseregistration.config;

import com.example.courseregistration.entity.Course;
import com.example.courseregistration.entity.User;
import com.example.courseregistration.enums.CourseStatus;
import com.example.courseregistration.enums.UserRole;
import com.example.courseregistration.service.CourseService;
import com.example.courseregistration.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Override
    public void run(String... args) throws Exception {
        initUsers();
        initCourses();
    }

    private void initUsers() {
        if (!userService.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setName("系统管理员");
            admin.setRole(UserRole.ADMIN);
            admin.setDepartment("信息中心");
            admin.setPhone("13800000000");
            admin.setEmail("admin@example.com");
            userService.save(admin);
        }

        if (!userService.existsByUsername("employee1")) {
            User employee1 = new User();
            employee1.setUsername("employee1");
            employee1.setPassword("123456");
            employee1.setName("张三");
            employee1.setRole(UserRole.EMPLOYEE);
            employee1.setDepartment("技术部");
            employee1.setPhone("13800000001");
            employee1.setEmail("zhangsan@example.com");
            userService.save(employee1);
        }

        if (!userService.existsByUsername("employee2")) {
            User employee2 = new User();
            employee2.setUsername("employee2");
            employee2.setPassword("123456");
            employee2.setName("李四");
            employee2.setRole(UserRole.EMPLOYEE);
            employee2.setDepartment("市场部");
            employee2.setPhone("13800000002");
            employee2.setEmail("lisi@example.com");
            userService.save(employee2);
        }

        if (!userService.existsByUsername("employee3")) {
            User employee3 = new User();
            employee3.setUsername("employee3");
            employee3.setPassword("123456");
            employee3.setName("王五");
            employee3.setRole(UserRole.EMPLOYEE);
            employee3.setDepartment("人事部");
            employee3.setPhone("13800000003");
            employee3.setEmail("wangwu@example.com");
            userService.save(employee3);
        }
    }

    private void initCourses() {
        if (courseService.findAll().isEmpty()) {
            Course course1 = new Course();
            course1.setName("Java 高级开发实战");
            course1.setType("技术培训");
            course1.setLecturer("李教授");
            course1.setStartTime(LocalDateTime.now().plusDays(7));
            course1.setEndTime(LocalDateTime.now().plusDays(7).plusHours(4));
            course1.setMaxCapacity(30);
            course1.setRegisteredCount(0);
            course1.setLocation("培训中心A101");
            course1.setDescription("本课程主要讲解Java高级特性，包括并发编程、JVM调优、微服务架构等内容，适合有一定Java基础的学员。");
            course1.setStatus(CourseStatus.PUBLISHED);
            course1.setCancelDeadline(LocalDateTime.now().plusDays(5));
            courseService.save(course1);

            Course course2 = new Course();
            course2.setName("项目管理实战");
            course2.setType("管理培训");
            course2.setLecturer("王经理");
            course2.setStartTime(LocalDateTime.now().plusDays(10));
            course2.setEndTime(LocalDateTime.now().plusDays(10).plusHours(6));
            course2.setMaxCapacity(20);
            course2.setRegisteredCount(0);
            course2.setLocation("培训中心B201");
            course2.setDescription("本课程涵盖项目管理全流程，包括需求分析、进度管控、风险管理、团队协作等核心内容。");
            course2.setStatus(CourseStatus.PUBLISHED);
            course2.setCancelDeadline(LocalDateTime.now().plusDays(8));
            courseService.save(course2);

            Course course3 = new Course();
            course3.setName("数据分析入门");
            course3.setType("技能培训");
            course3.setLecturer("张老师");
            course3.setStartTime(LocalDateTime.now().plusDays(3));
            course3.setEndTime(LocalDateTime.now().plusDays(3).plusHours(3));
            course3.setMaxCapacity(25);
            course3.setRegisteredCount(0);
            course3.setLocation("培训中心C301");
            course3.setDescription("零基础学习数据分析，掌握Excel高级功能、SQL查询、Python数据分析基础。");
            course3.setStatus(CourseStatus.PUBLISHED);
            course3.setCancelDeadline(LocalDateTime.now().plusDays(1));
            courseService.save(course3);

            Course course4 = new Course();
            course4.setName("沟通技巧提升");
            course4.setType("软技能");
            course4.setLecturer("刘讲师");
            course4.setStartTime(LocalDateTime.now().plusDays(14));
            course4.setEndTime(LocalDateTime.now().plusDays(14).plusHours(4));
            course4.setMaxCapacity(40);
            course4.setRegisteredCount(0);
            course4.setLocation("培训中心D401");
            course4.setDescription("提升职场沟通能力，包括向上沟通、向下沟通、跨部门沟通等技巧。");
            course4.setStatus(CourseStatus.DRAFT);
            courseService.save(course4);

            Course course5 = new Course();
            course5.setName("Vue3 前端开发");
            course5.setType("技术培训");
            course5.setLecturer("陈工程师");
            course5.setStartTime(LocalDateTime.now().minusDays(2));
            course5.setEndTime(LocalDateTime.now().minusDays(2).plusHours(4));
            course5.setMaxCapacity(30);
            course5.setRegisteredCount(15);
            course5.setLocation("培训中心A102");
            course5.setDescription("Vue3 最新特性与实战开发，适合前端开发人员。");
            course5.setStatus(CourseStatus.PUBLISHED);
            courseService.save(course5);
        }
    }
}
