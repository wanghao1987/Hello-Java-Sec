package com.best.hello.controller.SQLI;

import com.best.hello.entity.JsonResult;
import com.best.hello.util.Security;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.codecs.Codec;
import org.owasp.esapi.codecs.OracleCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;
import java.util.Map;

/**
 * SQL注入 - JDBC注入
 *
 * @author nul1
 * @date 2021/07/24
 * 审计的函数
 * 1. executeQuery
 * 2. prepareStatement
 * 3. queryForMap
 * 4. query
 */

@Api("SQL注入 - JDBC")
@RestController
@RequestMapping("/SQLI/JDBC")
public class JDBC {

    Logger log = LoggerFactory.getLogger(JDBC.class);

    @Value("${spring.datasource.url}")
    private String db_url;

    @Value("${spring.datasource.username}")
    private String db_user;

    @Value("${spring.datasource.password}")
    private String db_pass;

    
    /**
     * @poc http://127.0.0.1:8888/SQLI/JDBC/vul1?id=1' and updatexml(1,concat(0x7e,(SELECT user()),0x7e),1)--%20+
     * 
     * // aaaa' or 1=1 union select 1,'2
     */
   
    @RequestMapping("/vulJdbc")
	@ResponseBody
    public JsonResult vulJdbc(String username, String password) {

    	// 创建JsonResult对象
    			JsonResult jsonResult = new JsonResult();
    		    
    			Connection conn = null;
    			try {
//    			   
    			    conn =
    					       DriverManager.getConnection("jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf-8&serverTimezone=America/New_York&" +
    					                                   "user=root&password=root");

    			    
    			} catch (SQLException ex) {
    			    // handle any errors
    			    System.out.println("SQLException: " + ex.getMessage());
    			    System.out.println("SQLState: " + ex.getSQLState());
    			    System.out.println("VendorError: " + ex.getErrorCode());
    			}
    			
    			Statement stmt = null;
    			ResultSet rs = null;

    			try {
    				// aaaa' or 1=1 union select 1,'2
    			    stmt = conn.createStatement();
    			    String sql = "SELECT username, password FROM t_user where username = '" + username + "' and password = '" + password +"'";
    			    System.out.println(sql);
    			    rs = stmt.executeQuery(sql);
    			    
    			    if(rs.next()) {
    			    	jsonResult.setState(1);
    			    }else {
    			    	jsonResult.setState(2);
    			    }	
    			    jsonResult.setSql(sql);
    			    
    			}
    			catch (SQLException ex){
    			    // handle any errors
    			    System.out.println("SQLException: " + ex.getMessage());
    			    System.out.println("SQLState: " + ex.getSQLState());
    			    System.out.println("VendorError: " + ex.getErrorCode());
    			}
    			finally {
    			    // it is a good idea to release
    			    // resources in a finally{} block
    			    // in reverse-order of their creation
    			    // if they are no-longer needed

    			    if (rs != null) {
    			        try {
    			            rs.close();
    			        } catch (SQLException sqlEx) { } // ignore

    			        rs = null;
    			    }

    			    if (stmt != null) {
    			        try {
    			            stmt.close();
    			        } catch (SQLException sqlEx) { } // ignore

    			        stmt = null;
    			    }
    			}
    			
    			
    		    // 返回结果
    			return jsonResult;
    }

    
    
    /**
     * @poc http://127.0.0.1:8888/SQLI/JDBC/vul1?id=1' and updatexml(1,concat(0x7e,(SELECT user()),0x7e),1)--%20+
     */
    @ApiOperation(value = "vul: JDBC语句拼接")
    @GetMapping("/vul1")
    public String vul1(String id) {

        StringBuilder result = new StringBuilder();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(db_url, db_user, db_pass);

            Statement stmt = conn.createStatement();
            String sql = "select * from users where id = '" + id + "'";
            log.info("[vul] 执行SQL语句： " + sql);
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String res_name = rs.getString("user");
                String res_pass = rs.getString("pass");
                String info = String.format("查询结果 %s: %s", res_name, res_pass);
                result.append(info);
            }

            rs.close();
            stmt.close();
            conn.close();
            return result.toString();

        } catch (Exception e) {
            // 输出错误，用于报错注入
            return e.toString();
        }
    }


    /**
     * @poc http://127.0.0.1:8888/SQLI/JDBC/vul2?id=2%20and%201=1
     */
    @ApiOperation(value = "vul：JDBC预编译拼接", notes = "采用预编译的方法，但没使用?占位，此时进行预编译也无法阻止SQL注入")
    @GetMapping("/vul2")
    public String vul2(String id) {

        StringBuilder result = new StringBuilder();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(db_url, db_user, db_pass);

            String sql = "select * from users where id = " + id;
            log.info("[vul] 执行SQL语句： " + sql);
            PreparedStatement st = conn.prepareStatement(sql);
            ResultSet rs = st.executeQuery();

            while (rs.next()) {
                String res_name = rs.getString("user");
                String res_pass = rs.getString("pass");
                String info = String.format("查询结果%n %s: %s%n", res_name, res_pass);
                result.append(info);
            }

            rs.close();
            st.close();
            conn.close();
            return result.toString();

        } catch (Exception e) {
            return e.toString();
        }
    }


    @ApiOperation(value = "vul：JdbcTemplate", notes = "JDBCTemplate是Spring对JDBC的封装，使其更易于使用")
    @GetMapping("/vul3")
    public Map<String, Object> vul3(String id) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(db_url);
        dataSource.setUsername(db_user);
        dataSource.setPassword(db_pass);

        JdbcTemplate jdbctemplate = new JdbcTemplate(dataSource);

        String sql_vul = "select * from users where id = " + id;
        String sql_safe = "select * from users where id = ?";

        return jdbctemplate.queryForMap(sql_vul);
    }


    @ApiOperation(value = "safe：JDBC预编译", notes = "采用预编译的方法，使用?占位，也叫参数化的SQL")
    @GetMapping("/safe1")
    public String safe1(String id) {

        StringBuilder result = new StringBuilder();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(db_url, db_user, db_pass);

            String sql = "select * from users where id = ?";
            PreparedStatement st = conn.prepareStatement(sql);
            st.setString(1, id);
            log.info("[safe] 执行SQL语句： " + st);
            ResultSet rs = st.executeQuery();

            while (rs.next()) {
                String res_name = rs.getString("user");
                String res_pass = rs.getString("pass");
                String info = String.format("查询结果%n %s: %s%n", res_name, res_pass);
                result.append(info);
            }

            rs.close();
            st.close();
            conn.close();
            return result.toString();

        } catch (Exception e) {
            return e.toString();
        }
    }


    @ApiOperation(value = "safe：采用黑名单过滤的方式")
    @GetMapping("/safe2")
    public String safe2(String id) {

        if (!Security.checkSql(id)) {

            StringBuilder result = new StringBuilder();

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection conn = DriverManager.getConnection(db_url, db_user, db_pass);

                Statement stmt = conn.createStatement();
                String sql = "select * from users where id = '" + id + "'";
                ResultSet rs = stmt.executeQuery(sql);
                log.info("[safe] 执行SQL语句： " + sql);

                while (rs.next()) {
                    String res_name = rs.getString("user");
                    String res_pass = rs.getString("pass");
                    String info = String.format("查询结果%n %s: %s%n", res_name, res_pass);
                    result.append(info);
                }

                rs.close();
                stmt.close();
                conn.close();
                return result.toString();

            } catch (Exception e) {
                return e.toString();
            }
        } else {
            log.warn("检测到非法注入");
            return "检测到非法注入！";
        }
    }


    @ApiOperation(value = "safe：采用ESAPI过滤")
    @GetMapping("/safe3")
    public String safe3(String id) {
        StringBuilder result = new StringBuilder();

        try {
            Codec<Character> oracleCodec = new OracleCodec();
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(db_url, db_user, db_pass);

            Statement stmt = conn.createStatement();
            String sql = "select * from users where id = '" + ESAPI.encoder().encodeForSQL(oracleCodec, id) + "'";
            log.info("[safe] 执行SQL语句： " + sql);
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String res_name = rs.getString("user");
                String res_pass = rs.getString("pass");
                String info = String.format("查询结果%n %s: %s%n", res_name, res_pass);
                result.append(info);
            }

            rs.close();
            stmt.close();
            conn.close();
            return result.toString();

        } catch (Exception e) {
            return e.toString();
        }
    }


    @ApiOperation(value = "safe：强制数据类型")
    @GetMapping("/safe4")
    public Map<String, Object> safe4(Integer id) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(db_url);
        dataSource.setUsername(db_user);
        dataSource.setPassword(db_pass);

        JdbcTemplate jdbctemplate = new JdbcTemplate(dataSource);

        String sql_vul = "select * from users where id = " + id;

        return jdbctemplate.queryForMap(sql_vul);
    }

}
