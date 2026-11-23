<template>
  <div class="auth-page">
    <div class="scrim"></div>
    <div class="orb orb-a"></div>
    <div class="orb orb-b"></div>
    <div class="orb orb-c"></div>

    <div class="auth-wrap">
      <section class="brand">
        <div class="brand-top">
          <div class="brand-mark">
            <img :src="logoImage" alt="广州大学校徽" />
          </div>
          <div>
            <div class="brand-school">GUANGZHOU UNIVERSITY</div>
            <div class="brand-cn">广州大学</div>
          </div>
        </div>

        <h1>宿舍报修<br />与工单管理系统</h1>
        <p class="brand-desc">
          面向广州大学学生、维修人员与后勤管理员的宿舍报修服务平台，
          从报修提交、派单调度到维修验收，全流程线上闭环。
        </p>

        <div class="brand-foot">广州大学 · 后勤服务保障</div>
      </section>

      <section class="auth-card">
        <slot />
      </section>
    </div>
  </div>
</template>

<script setup>
import logoImage from '../assets/logo.jpg'
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  position: relative;
  overflow: hidden;
  background: #0a1730 url('../assets/bg.jpg') center / cover no-repeat;
}

/* 深蓝遮罩：背景图上方为浅蓝天空、下方为深绿植被，用偏蓝的暗色渐变压住亮部，让浅色文字始终清晰 */
.scrim {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    linear-gradient(112deg, rgba(6, 20, 45, 0.93) 0%, rgba(8, 27, 58, 0.8) 34%, rgba(9, 31, 66, 0.42) 64%, rgba(6, 18, 40, 0.66) 100%),
    linear-gradient(180deg, rgba(6, 18, 40, 0.32) 0%, rgba(6, 18, 40, 0) 32%, rgba(4, 14, 32, 0.55) 100%);
}

.orb {
  position: absolute;
  z-index: 1;
  border-radius: 50%;
  filter: blur(2px);
  opacity: 0.35;
  pointer-events: none;
}

.orb-a {
  width: 420px;
  height: 420px;
  left: -120px;
  top: -140px;
  background: radial-gradient(circle at 30% 30%, rgba(120, 178, 255, 0.45), transparent 65%);
}

.orb-b {
  width: 520px;
  height: 520px;
  right: -160px;
  bottom: -220px;
  background: radial-gradient(circle at 60% 40%, rgba(38, 198, 172, 0.32), transparent 65%);
}

.orb-c {
  width: 220px;
  height: 220px;
  right: 24%;
  top: -80px;
  background: radial-gradient(circle at 50% 50%, rgba(150, 190, 255, 0.28), transparent 70%);
}

.auth-wrap {
  position: relative;
  z-index: 2;
  width: min(1080px, 100%);
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 46px;
  align-items: center;
}

.brand {
  color: #f4f8ff;
  padding: 10px 6px;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 38px;
}

.brand-mark {
  width: 58px;
  height: 58px;
  flex-shrink: 0;
  border-radius: 16px;
  padding: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(2, 10, 30, 0.45), 0 0 0 1px rgba(255, 255, 255, 0.55) inset;
}

.brand-mark img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.brand-school {
  font-size: 11px;
  letter-spacing: 3px;
  color: rgba(186, 212, 252, 0.85);
}

.brand-cn {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 4px;
  color: #ffffff;
  text-shadow: 0 2px 12px rgba(2, 10, 26, 0.45);
}

.brand h1 {
  margin: 0;
  font-size: 46px;
  line-height: 1.22;
  letter-spacing: 1px;
  font-weight: 800;
  background: linear-gradient(118deg, #ffffff 8%, #dbe9ff 55%, #a6c9ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 6px 22px rgba(2, 10, 26, 0.55));
}

.brand-desc {
  margin: 20px 0 30px;
  max-width: 480px;
  line-height: 1.9;
  color: rgba(226, 238, 255, 0.86);
  font-size: 14px;
  text-shadow: 0 1px 10px rgba(2, 10, 26, 0.45);
}

.brand-foot {
  margin-top: 46px;
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(188, 210, 245, 0.62);
}

.auth-card {
  position: relative;
  overflow: hidden;
  background: #ffffff;
  border-radius: 22px;
  padding: 40px 38px 34px;
  box-shadow: 0 30px 80px rgba(2, 10, 30, 0.45);
}

.auth-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #1f5fd6, #4f9cff, #34c6b4);
}

@media (max-width: 920px) {
  .auth-wrap {
    grid-template-columns: 1fr;
    max-width: 460px;
    gap: 28px;
  }

  .brand {
    display: none;
  }

  .auth-card {
    padding: 32px 26px 28px;
  }
}
</style>
