package com.junchen.common.handler;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL jsonb/json 列专用的 Fastjson 类型处理器。
 *
 * <p>MySQL 的 json 列可直接接受 JSON 字符串参数，而 PG 的 jsonb 列要求参数类型为
 * {@code PGobject}（否则报 "column is of type jsonb but expression is of type character varying"）。
 * 本处理器只覆盖写入逻辑：把 Fastjson 序列化后的字符串包装成 PGobject；
 * 读取仍沿用 PgJsonbTypeHandler（PG 返回的 jsonb 是字符串）。</p>
 */
public class PgJsonbTypeHandler extends FastjsonTypeHandler {

    /** MyBatis-Plus 通过带 Class 参数的构造器实例化 TableField 上声明的 TypeHandler。 */
    public PgJsonbTypeHandler(Class<?> type) {
        super(type);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
            throws SQLException {
        String json = super.toJson(parameter);
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue(json);
        ps.setObject(i, pgObject);
    }
}
