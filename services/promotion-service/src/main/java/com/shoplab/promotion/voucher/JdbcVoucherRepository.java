package com.shoplab.promotion.voucher;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Cài đặt JDBC của {@link VoucherRepository}. */
@Repository
public class JdbcVoucherRepository implements VoucherRepository {

  private static final String SELECT =
      """
      select id, code, type, value, max_discount, min_order, quota, used, locked,
             start_at, end_at, created_at
        from vouchers
      """;

  private final JdbcClient jdbc;

  public JdbcVoucherRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<VoucherRecord> findByCode(String code) {
    return jdbc.sql(SELECT + " where code = :code").param("code", code).query(this::map).optional();
  }

  @Override
  public Optional<VoucherRecord> findById(UUID id) {
    return jdbc.sql(SELECT + " where id = :id").param("id", id).query(this::map).optional();
  }

  @Override
  public void insert(VoucherRecord voucher) {
    jdbc.sql(
            """
            insert into vouchers
              (id, code, type, value, max_discount, min_order, quota, used, locked,
               start_at, end_at, created_at)
            values
              (:id, :code, :type, :value, :maxDiscount, :minOrder, :quota, 0, 0,
               :startAt, :endAt, :createdAt)
            """)
        .param("id", voucher.id())
        .param("code", voucher.code())
        .param("type", voucher.type().name())
        .param("value", voucher.value())
        .param("maxDiscount", voucher.maxDiscount(), java.sql.Types.NUMERIC)
        .param("minOrder", voucher.minOrder())
        .param("quota", voucher.quota())
        .param("startAt", Timestamp.from(voucher.startAt()))
        .param("endAt", Timestamp.from(voucher.endAt()))
        .param("createdAt", Timestamp.from(voucher.createdAt()))
        .update();
  }

  /**
   * Điều kiện {@code used + locked < quota} + cửa sổ thời gian nằm <b>trong</b> câu UPDATE ⇒ hai
   * request đồng thời không thể cùng giữ lượt cuối. {@code 0 row}` là tín hiệu "hết lượt hoặc ngoài
   * giờ" (service đọc lại voucher để nói rõ lý do).
   */
  @Override
  public int lock(String code, Instant now) {
    return jdbc.sql(
            """
            update vouchers
               set locked = locked + 1
             where code = :code
               and used + locked < quota
               and start_at <= :now and :now < end_at
            """)
        .param("code", code)
        .param("now", Timestamp.from(now))
        .update();
  }

  @Override
  public int commit(UUID voucherId) {
    return jdbc.sql(
            """
            update vouchers set used = used + 1, locked = locked - 1
             where id = :id and locked >= 1
            """)
        .param("id", voucherId)
        .update();
  }

  @Override
  public int release(UUID voucherId) {
    return jdbc.sql("update vouchers set locked = locked - 1 where id = :id and locked >= 1")
        .param("id", voucherId)
        .update();
  }

  private VoucherRecord map(ResultSet rs, int rowNum) throws SQLException {
    BigDecimal maxDiscount = rs.getBigDecimal("max_discount");
    return new VoucherRecord(
        rs.getObject("id", UUID.class),
        rs.getString("code"),
        VoucherType.valueOf(rs.getString("type")),
        rs.getBigDecimal("value"),
        maxDiscount,
        rs.getBigDecimal("min_order"),
        rs.getInt("quota"),
        rs.getInt("used"),
        rs.getInt("locked"),
        rs.getTimestamp("start_at").toInstant(),
        rs.getTimestamp("end_at").toInstant(),
        rs.getTimestamp("created_at").toInstant());
  }
}
