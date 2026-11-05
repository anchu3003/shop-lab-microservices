package com.shoplab.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.commons.error.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DebugControllerTest {

  private final DebugController controller = new DebugController();

  @Test
  @DisplayName("kẹp giá trị âm về 0 (không cho sleep âm gây lỗi)")
  void shouldClampNegativeToZero() {
    assertThat(DebugController.clamp(-1)).isZero();
    assertThat(DebugController.clamp(Long.MIN_VALUE)).isZero();
  }

  @Test
  @DisplayName("giá trị trong khoảng hợp lệ được giữ nguyên")
  void shouldKeepValueInRange() {
    assertThat(DebugController.clamp(0)).isZero();
    assertThat(DebugController.clamp(1_500)).isEqualTo(1_500);
    assertThat(DebugController.clamp(DebugController.MAX_SLEEP_MS))
        .isEqualTo(DebugController.MAX_SLEEP_MS);
  }

  @Test
  @DisplayName("giá trị vượt trần bị kẹp về trần — không cho giữ request vô hạn")
  void shouldClampAboveMaximum() {
    assertThat(DebugController.clamp(DebugController.MAX_SLEEP_MS + 1))
        .isEqualTo(DebugController.MAX_SLEEP_MS);
    assertThat(DebugController.clamp(Long.MAX_VALUE)).isEqualTo(DebugController.MAX_SLEEP_MS);
  }

  @Test
  @DisplayName("/debug/slow trả về số ms thực sự đã chờ")
  void shouldReportActualSleepDuration() throws InterruptedException {
    assertThat(controller.slow(1)).containsEntry("sleptMs", 1L);
    assertThat(controller.slow(0)).containsEntry("sleptMs", 0L);
  }

  @Test
  @DisplayName("/debug/error ném NotFoundException để đi qua nhánh lỗi nghiệp vụ")
  void shouldThrowNotFoundFromErrorEndpoint() {
    assertThatThrownBy(controller::error).isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("/debug/boom ném lỗi không lường trước để kiểm tra nhánh 500")
  void shouldThrowUnexpectedFromBoomEndpoint() {
    assertThatThrownBy(controller::boom).isInstanceOf(IllegalStateException.class);
  }
}
