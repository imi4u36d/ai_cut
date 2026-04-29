<template>
  <section class="workflow-view">
    <aside class="workflow-rail">
      <div class="workflow-rail-flip-shell">
        <div class="workflow-rail-flip" :class="{ 'workflow-rail-flip-active': createReviewFlipped }">
          <section class="surface-panel workflow-panel workflow-rail-panel workflow-rail-flip__face workflow-rail-flip__face-front">
            <p v-if="createError" class="workflow-error">{{ createError }}</p>

            <div class="workflow-rail__actions workflow-rail__actions-inline">
              <button class="btn-secondary btn-sm" type="button" :disabled="loadingWorkflows" @click="loadWorkflows">
                {{ loadingWorkflows ? "刷新中..." : "刷新" }}
              </button>
              <button
                class="btn-primary btn-sm workflow-submit"
                type="button"
                :disabled="creatingWorkflow || loadingOptions"
                @click="startCreateWorkflow"
              >
                {{ creatingWorkflow ? "创建中..." : "新建工作流" }}
              </button>
            </div>

            <label class="workflow-rail__search">
              <span>搜索工作流</span>
              <input v-model="workflowSearch" class="field-input" type="search" placeholder="按标题、阶段、状态筛选" />
            </label>

            <div class="workflow-list-shell">
              <p v-if="listError" class="workflow-error">{{ listError }}</p>
              <div v-else-if="loadingWorkflows" class="workflow-empty">正在加载工作流...</div>
              <div v-else-if="!filteredWorkflows.length" class="workflow-empty">
                {{ workflows.length ? "没有匹配的工作流" : "还没有阶段工作流" }}
              </div>
              <div v-else class="workflow-list">
                <article
                  v-for="item in filteredWorkflows"
                  :key="item.id"
                  class="workflow-list__item"
                  :class="{ 'workflow-list__item-active': item.id === selectedWorkflowId }"
                >
                  <button type="button" class="workflow-list__open" @click="openWorkflow(item.id, item.currentStage)">
                    <div class="workflow-list__top">
                      <strong>{{ item.title }}</strong>
                      <span class="surface-chip">{{ item.currentStage }}</span>
                    </div>
                    <div class="workflow-list__meta">
                      <span>{{ item.aspectRatio }}</span>
                      <span>分镜 {{ item.storyboardVersionCount }}</span>
                      <span>关键帧 {{ item.keyframeVersionCount }}</span>
                      <span>视频 {{ item.videoVersionCount }}</span>
                    </div>
                    <div class="workflow-list__meta">
                      <span>{{ item.status }}</span>
                      <span>评分 {{ ratingLabel(item.effectRating) }}</span>
                    </div>
                    <div class="workflow-list__meta">
                      <span>{{ formatDateTime(item.updatedAt) }}</span>
                    </div>
                  </button>
                  <div class="workflow-list__actions">
                    <button
                      class="btn-danger btn-sm"
                      type="button"
                      :disabled="busyActionKey === `delete-workflow-${item.id}`"
                      @click="handleDeleteWorkflow(item)"
                    >
                      {{ busyActionKey === `delete-workflow-${item.id}` ? "删除中..." : "删除" }}
                    </button>
                  </div>
                </article>
              </div>
            </div>
          </section>

          <section class="surface-panel workflow-panel workflow-rail-panel workflow-rail-flip__face workflow-rail-flip__face-back">
            <div class="workflow-review-card workflow-review-card-rail">
              <div class="stage-config-card__head workflow-review-card__head">
                <div>
                  <h3>创建前检查</h3>
                </div>
                <div class="workflow-review-card__status">
                  <span class="surface-chip">必填完成 {{ createReviewConfiguredCount }} / {{ createReviewRequiredItems.length }}</span>
                  <span class="surface-chip" :class="{ 'workflow-review-chip-warning': !canSubmitCreateReview }">
                    {{ canSubmitCreateReview ? "可以保存" : "仍有必填项缺失" }}
                  </span>
                </div>
              </div>

              <p v-if="createError" class="workflow-error">{{ createError }}</p>

              <div class="workflow-review-grid workflow-review-grid-rail">
                <article v-for="section in createReviewSections" :key="section.key" class="workflow-review-section">
                  <div class="workflow-review-section__head">
                    <p class="workflow-eyebrow">{{ section.eyebrow }}</p>
                    <h4>{{ section.title }}</h4>
                  </div>

                  <div class="workflow-review-list">
                    <div v-for="item in section.items" :key="item.key" class="workflow-review-item">
                      <div>
                        <strong>{{ item.label }}</strong>
                        <p>{{ item.valueLabel }}</p>
                      </div>
                      <div class="workflow-review-item__meta">
                        <span class="surface-chip">{{ item.required ? "必填" : "可选" }}</span>
                        <span
                          class="surface-chip"
                          :class="item.configured ? 'workflow-review-chip-success' : (item.required ? 'workflow-review-chip-warning' : 'workflow-review-chip-muted')"
                        >
                          {{ item.configured ? "已配置" : (item.required ? "未配置" : "未填写") }}
                        </span>
                      </div>
                    </div>
                  </div>
                </article>
              </div>

              <div class="workflow-review-actions workflow-review-actions-rail">
                <button class="btn-secondary btn-sm" type="button" :disabled="creatingWorkflow" @click="closeCreateReview">
                  取消
                </button>
                <button
                  class="btn-primary btn-sm"
                  type="button"
                  :disabled="creatingWorkflow || !canSubmitCreateReview"
                  @click="handleCreateWorkflow"
                >
                  {{ creatingWorkflow ? "保存中..." : "保存工作流" }}
                </button>
              </div>
            </div>
          </section>
        </div>
      </div>
    </aside>

    <section class="workflow-main">
      <section v-if="createComposerVisible" class="surface-panel workflow-panel workflow-create-shell">
        <div class="workflow-create-shell__grid">
          <div class="workflow-create-canvas">
            <form id="workflow-create-form" class="workflow-form workflow-stage-create" @submit.prevent>
              <section class="surface-tile stage-config-card stage-config-card--base">
                <div class="stage-config-card__head">
                  <div class="stage-config-card__title-row">
                    <div>
                      <p class="workflow-eyebrow">Workflow Base</p>
                      <h3>工作流基础信息</h3>
                    </div>

                    <div class="stage-switch-row">
                      <button
                        v-for="stage in createStageOptions"
                        :key="stage.key"
                        type="button"
                        class="stage-switch-btn"
                        :class="{ 'stage-switch-btn-active': activeCreateStage === stage.key }"
                        @click="activeCreateStage = stage.key"
                      >
                        {{ stage.shortLabel }}
                      </button>
                    </div>
                  </div>
                </div>

                <div class="stage-config-fields stage-config-fields--single">
                  <label class="workflow-field stage-config-field-title">
                    <span>标题</span>
                    <input v-model="createForm.title" class="field-input" required placeholder="例如：第 12 集阶段拆分版" />
                  </label>

                  <label class="workflow-field stage-config-field-body">
                    <span>正文</span>
                    <textarea
                      v-model="createForm.transcriptText"
                      class="field-textarea"
                      rows="5"
                      placeholder="输入小说正文或脚本内容"
                    ></textarea>
                  </label>

                  <label class="workflow-field stage-config-field-prompt">
                    <span>全局 Prompt</span>
                    <textarea
                      v-model="createForm.globalPrompt"
                      class="field-textarea"
                      rows="4"
                      placeholder="填写这一轮工作流的总体创作要求"
                    ></textarea>
                  </label>
                </div>
              </section>

              <section class="surface-tile stage-config-card stage-config-card--focus">
                <div class="stage-config-card__head">
                  <div>
                    <p class="workflow-eyebrow">{{ activeCreateStageMeta.label }}</p>
                    <h3>{{ activeCreateStageMeta.description }}</h3>
                  </div>
                  <span class="surface-chip">参数步骤 {{ activeStageFieldIndex + 1 }} / {{ activeStageSteps.length }}</span>
                </div>

                <div class="stage-progress-form">
                  <div class="stage-progress-form__rail">
                    <div class="stage-progress-form__bar">
                      <span class="stage-progress-form__fill" :style="{ width: `${activeStageProgress}%` }"></span>
                    </div>
                    <div class="stage-progress-form__steps">
                      <button
                        v-for="(step, index) in activeStageSteps"
                        :key="step.key"
                        type="button"
                        class="stage-progress-form__step"
                        :class="{
                          'stage-progress-form__step-active': index === activeStageFieldIndex,
                          'stage-progress-form__step-done': index < activeStageFieldIndex,
                        }"
                        @click="setStageFieldIndex(index)"
                      >
                        <span>{{ index + 1 }}</span>
                        <strong>{{ step.label }}</strong>
                      </button>
                    </div>
                  </div>

                  <div class="stage-progress-form__panel">
                    <div class="stage-progress-form__panel-head">
                      <div>
                        <h4>{{ currentStageField?.label }}</h4>
                      </div>
                      <span class="surface-chip">{{ currentStageField?.valueLabel }}</span>
                    </div>

                    <div v-if="currentStageField?.key === 'textAnalysisModel'" class="stage-progress-form__field">
                      <span>选择文本分镜模型</span>
                      <div class="stage-option-grid">
                        <button
                          v-for="item in textModelOptions"
                          :key="item.value"
                          type="button"
                          class="stage-option-card"
                          :class="{ 'stage-option-card-active': createForm.textAnalysisModel === item.value }"
                          @click="createForm.textAnalysisModel = item.value"
                        >
                          <strong>{{ item.label }}</strong>
                          <span>{{ item.description || item.provider || item.value }}</span>
                        </button>
                      </div>
                    </div>

                    <div v-else-if="currentStageField?.key === 'storyboardDurationSeconds'" class="stage-progress-form__field stage-progress-form__field-slider">
                      <span>选择镜头时长模式</span>
                      <div class="stage-toggle-row">
                        <button
                          type="button"
                          class="stage-toggle-chip"
                          :class="{ 'stage-toggle-chip-active': storyboardDurationMode === 'auto' }"
                          @click="storyboardDurationMode = 'auto'"
                        >
                          自动
                        </button>
                        <button
                          type="button"
                          class="stage-toggle-chip"
                          :class="{ 'stage-toggle-chip-active': storyboardDurationMode === 'manual' }"
                          @click="storyboardDurationMode = 'manual'"
                        >
                          手动
                        </button>
                      </div>
                      <p class="stage-progress-form__field-note">
                        {{ storyboardDurationMode === "auto" ? "自动模式会沿用当前工作流的默认时长策略。" : storyboardManualDurationHint }}
                      </p>
                      <template v-if="storyboardDurationMode === 'manual'">
                        <input
                          v-model="storyboardManualDurationSeconds"
                          class="stage-slider__range"
                          type="range"
                          :min="STORYBOARD_MANUAL_DURATION_MIN_SECONDS"
                          :max="STORYBOARD_MANUAL_DURATION_MAX_SECONDS"
                          step="1"
                        />
                        <div class="stage-slider__meta">
                          <strong>{{ storyboardManualDurationSeconds || String(STORYBOARD_MANUAL_DURATION_MIN_SECONDS) }} 秒</strong>
                          <input
                            v-model="storyboardManualDurationSeconds"
                            class="field-input stage-slider__number"
                            type="number"
                            :min="STORYBOARD_MANUAL_DURATION_MIN_SECONDS"
                            :max="STORYBOARD_MANUAL_DURATION_MAX_SECONDS"
                            step="1"
                          />
                        </div>
                        <p v-if="storyboardManualDurationValidationMessage" class="stage-progress-form__field-error">
                          {{ storyboardManualDurationValidationMessage }}
                        </p>
                      </template>
                    </div>

                    <div v-else-if="currentStageField?.key === 'keyframeSeed'" class="stage-progress-form__field stage-progress-form__field-slider">
                      <span>选择 Seed 模式</span>
                      <div class="stage-toggle-row">
                        <button
                          type="button"
                          class="stage-toggle-chip"
                          :class="{ 'stage-toggle-chip-active': createForm.keyframeSeed === '' }"
                          @click="createForm.keyframeSeed = ''"
                        >
                          自动
                        </button>
                        <button
                          type="button"
                          class="stage-toggle-chip"
                          :class="{ 'stage-toggle-chip-active': createForm.keyframeSeed !== '' }"
                          @click="createForm.keyframeSeed = createForm.keyframeSeed || '1024'"
                        >
                          固定 Seed
                        </button>
                      </div>
                      <template v-if="createForm.keyframeSeed !== ''">
                        <input
                          v-model="createForm.keyframeSeed"
                          class="stage-slider__range"
                          type="range"
                          min="0"
                          max="9999"
                          step="1"
                        />
                        <div class="stage-slider__meta">
                          <strong>{{ createForm.keyframeSeed }}</strong>
                          <input
                            v-model="createForm.keyframeSeed"
                            class="field-input stage-slider__number"
                            type="number"
                            min="0"
                            max="9999"
                            step="1"
                          />
                        </div>
                      </template>
                    </div>

                    <div v-else-if="currentStageField?.key === 'imageModel'" class="stage-progress-form__field">
                      <span>选择关键帧模型</span>
                      <div class="stage-option-grid">
                        <button
                          v-for="item in imageModelOptions"
                          :key="item.value"
                          type="button"
                          class="stage-option-card"
                          :class="{ 'stage-option-card-active': createForm.imageModel === item.value }"
                          @click="createForm.imageModel = item.value"
                        >
                          <strong>{{ item.label }}</strong>
                          <span>{{ item.description || item.provider || item.value }}</span>
                        </button>
                      </div>
                    </div>

                    <div v-else-if="currentStageField?.key === 'stylePreset'" class="stage-progress-form__field">
                      <span>选择视觉风格</span>
                      <div class="stage-option-grid">
                        <button
                          v-for="item in stylePresetOptions"
                          :key="item.key"
                          type="button"
                          class="stage-option-card"
                          :class="{ 'stage-option-card-active': createForm.stylePreset === item.key }"
                          @click="createForm.stylePreset = item.key"
                        >
                          <strong>{{ item.label }}</strong>
                          <span>{{ item.description || item.key }}</span>
                        </button>
                      </div>
                    </div>

                    <div v-else-if="currentStageField?.key === 'aspectRatio'" class="stage-progress-form__field">
                      <span>选择画面比例</span>
                      <div class="stage-option-grid stage-option-grid--compact">
                        <button
                          v-for="item in aspectRatioOptions"
                          :key="item.value"
                          type="button"
                          class="stage-option-card"
                          :class="{ 'stage-option-card-active': createForm.aspectRatio === item.value }"
                          @click="createForm.aspectRatio = item.value"
                        >
                          <strong>{{ item.label }}</strong>
                          <span>输出画面比例</span>
                        </button>
                      </div>
                    </div>

                    <div v-else-if="currentStageField?.key === 'videoModel'" class="stage-progress-form__field">
                      <span>选择视频模型</span>
                      <div class="stage-option-grid">
                        <button
                          v-for="item in videoModelOptions"
                          :key="item.value"
                          type="button"
                          class="stage-option-card"
                          :class="{ 'stage-option-card-active': createForm.videoModel === item.value }"
                          @click="createForm.videoModel = item.value"
                        >
                          <strong>{{ item.label }}</strong>
                          <span>{{ item.description || item.provider || item.value }}</span>
                        </button>
                      </div>
                    </div>

                    <div v-else-if="currentStageField?.key === 'videoSize'" class="stage-progress-form__field">
                      <span>选择输出尺寸</span>
                      <div class="stage-option-grid stage-option-grid--compact">
                        <button
                          v-for="item in videoSizeOptions"
                          :key="item.value"
                          type="button"
                          class="stage-option-card"
                          :class="{ 'stage-option-card-active': createForm.videoSize === item.value }"
                          @click="createForm.videoSize = item.value"
                        >
                          <strong>{{ item.label }}</strong>
                          <span>{{ item.width && item.height ? `${item.width} × ${item.height}` : item.value }}</span>
                        </button>
                      </div>
                    </div>

                    <div v-else-if="currentStageField?.key === 'videoSeed'" class="stage-progress-form__field stage-progress-form__field-slider">
                      <span>选择 Seed 模式</span>
                      <div class="stage-toggle-row">
                        <button
                          type="button"
                          class="stage-toggle-chip"
                          :class="{ 'stage-toggle-chip-active': createForm.videoSeed === '' }"
                          @click="createForm.videoSeed = ''"
                        >
                          自动
                        </button>
                        <button
                          type="button"
                          class="stage-toggle-chip"
                          :class="{ 'stage-toggle-chip-active': createForm.videoSeed !== '' }"
                          @click="createForm.videoSeed = createForm.videoSeed || '1024'"
                        >
                          固定 Seed
                        </button>
                      </div>
                      <template v-if="createForm.videoSeed !== ''">
                        <input
                          v-model="createForm.videoSeed"
                          class="stage-slider__range"
                          type="range"
                          min="0"
                          max="9999"
                          step="1"
                        />
                        <div class="stage-slider__meta">
                          <strong>{{ createForm.videoSeed }}</strong>
                          <input
                            v-model="createForm.videoSeed"
                            class="field-input stage-slider__number"
                            type="number"
                            min="0"
                            max="9999"
                            step="1"
                          />
                        </div>
                      </template>
                    </div>

                    <div class="stage-progress-form__actions">
                      <button
                        class="btn-secondary btn-sm"
                        type="button"
                        :disabled="activeStageFieldIndex === 0"
                        @click="previousStageField"
                      >
                        上一步
                      </button>
                      <button class="btn-secondary btn-sm" type="button" @click="nextStageField">
                        {{ activeStageFieldIndex >= activeStageSteps.length - 1 ? "回到第一项" : "下一步" }}
                      </button>
                    </div>
                  </div>

                  <div class="stage-progress-form__summary">
                    <button
                      v-for="(step, index) in activeStageSteps"
                      :key="`${activeCreateStage}-${step.key}`"
                      type="button"
                      class="stage-summary-card"
                      :class="{ 'stage-summary-card-active': index === activeStageFieldIndex }"
                      @click="setStageFieldIndex(index)"
                    >
                      <span>{{ step.label }}</span>
                      <strong>{{ step.valueLabel }}</strong>
                    </button>
                  </div>
                </div>
              </section>
            </form>
          </div>
        </div>
      </section>

      <div v-if="detailError" class="surface-panel workflow-banner workflow-banner-error">
        <p>{{ detailError }}</p>
        <button class="btn-secondary btn-sm" type="button" :disabled="loadingDetail" @click="reloadCurrentWorkflow">重新加载</button>
      </div>

      <div v-if="loadingDetail" class="surface-panel workflow-empty">
        正在加载工作流详情...
      </div>

      <template v-else-if="selectedWorkflow">
        <section class="surface-panel workflow-settings-panel">
          <div class="workflow-settings-panel__head">
            <div>
              <p class="workflow-eyebrow">Workflow Settings</p>
              <h2>工作流设置</h2>
              <div class="workflow-summary__meta">
                <span class="surface-chip">{{ valueOptionLabel(aspectRatioOptions, selectedWorkflow.aspectRatio, selectedWorkflow.aspectRatio || "未设置") }}</span>
                <span class="surface-chip">{{ keyOptionLabel(stylePresetOptions, selectedWorkflow.stylePreset, selectedWorkflow.stylePreset || "未设置") }}</span>
                <span class="surface-chip">{{ valueOptionLabel(catalogVideoSizeOptions, selectedWorkflow.videoSize, selectedWorkflow.videoSize || "未设置") }}</span>
              </div>
            </div>
            <button class="btn-secondary btn-sm" type="button" @click="workflowSettingsOpen = !workflowSettingsOpen">
              {{ workflowSettingsOpen ? "收起设置" : "编辑设置" }}
            </button>
          </div>

          <form v-if="workflowSettingsOpen" class="workflow-settings-form" @submit.prevent="handleUpdateWorkflowSettings">
            <label class="workflow-field workflow-settings-field-model">
              <span>文本模型</span>
              <select v-model="workflowSettingsDraft.textAnalysisModel" class="field-input">
                <option v-for="item in textModelOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label class="workflow-field workflow-settings-field-model">
              <span>关键帧模型</span>
              <select v-model="workflowSettingsDraft.imageModel" class="field-input">
                <option v-for="item in imageModelOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label class="workflow-field workflow-settings-field-model">
              <span>视频模型</span>
              <select v-model="workflowSettingsDraft.videoModel" class="field-input">
                <option v-for="item in videoModelOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label class="workflow-field workflow-settings-field-compact">
              <span>视觉风格</span>
              <select v-model="workflowSettingsDraft.stylePreset" class="field-input">
                <option v-for="item in stylePresetOptions" :key="item.key" :value="item.key">{{ item.label }}</option>
              </select>
            </label>
            <label class="workflow-field workflow-settings-field-short">
              <span>长宽比</span>
              <select v-model="workflowSettingsDraft.aspectRatio" class="field-input">
                <option v-for="item in aspectRatioOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label class="workflow-field workflow-settings-field-short">
              <span>关键帧 Seed</span>
              <input v-model="workflowSettingsDraft.keyframeSeed" class="field-input" type="number" min="0" max="999999999" placeholder="自动" />
            </label>
            <label class="workflow-field workflow-settings-field-compact">
              <span>输出尺寸</span>
              <select v-model="workflowSettingsDraft.videoSize" class="field-input">
                <option v-for="item in workflowSettingsVideoSizeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label class="workflow-field workflow-settings-field-short">
              <span>视频 Seed</span>
              <input v-model="workflowSettingsDraft.videoSeed" class="field-input" type="number" min="0" max="999999999" placeholder="自动" />
            </label>
            <div class="workflow-field workflow-settings-field-mode">
              <span>镜头时长模式</span>
              <div class="stage-toggle-row">
                <button
                  type="button"
                  class="stage-toggle-chip"
                  :class="{ 'stage-toggle-chip-active': workflowSettingsDraft.durationMode === 'auto' }"
                  @click="workflowSettingsDraft.durationMode = 'auto'"
                >
                  自动
                </button>
                <button
                  type="button"
                  class="stage-toggle-chip"
                  :class="{ 'stage-toggle-chip-active': workflowSettingsDraft.durationMode === 'manual' }"
                  @click="workflowSettingsDraft.durationMode = 'manual'"
                >
                  手动
                </button>
              </div>
            </div>
            <label class="workflow-field workflow-settings-field-duration">
              <span>最小时长</span>
              <input v-model="workflowSettingsDraft.minDurationSeconds" class="field-input" type="number" min="1" max="60" step="1" :disabled="workflowSettingsDraft.durationMode === 'auto'" />
            </label>
            <label class="workflow-field workflow-settings-field-duration">
              <span>最大时长</span>
              <input v-model="workflowSettingsDraft.maxDurationSeconds" class="field-input" type="number" min="1" max="60" step="1" :disabled="workflowSettingsDraft.durationMode === 'auto'" />
            </label>
            <div class="workflow-settings-form__actions">
              <span v-if="workflowSettingsValidationMessage" class="stage-progress-form__field-error">{{ workflowSettingsValidationMessage }}</span>
              <button class="btn-primary btn-sm" type="submit" :disabled="busyActionKey === 'workflow-settings' || Boolean(workflowSettingsValidationMessage)">
                {{ busyActionKey === "workflow-settings" ? "保存中..." : "保存设置" }}
              </button>
            </div>
          </form>
        </section>

        <section v-if="activeCreateStage === 'storyboard'" class="surface-panel workflow-panel workflow-stage-panel">
          <div class="workflow-panel__head">
            <div>
              <p class="workflow-eyebrow">Stage 1</p>
              <h2>文本分镜模块</h2>
              <div class="workflow-summary__meta">
                <span class="surface-chip">{{ selectedWorkflow.title }}</span>
                <span class="surface-chip">{{ selectedWorkflow.status }}</span>
                <span class="surface-chip">{{ selectedWorkflow.currentStage }}</span>
                <span class="surface-chip">{{ selectedWorkflow.aspectRatio }}</span>
              </div>
            </div>
            <div class="workflow-stage-panel__actions">
              <div class="stage-switch-row">
                <button
                  v-for="stage in createStageOptions"
                  :key="`detail-${stage.key}`"
                  type="button"
                  class="stage-switch-btn"
                  :class="{ 'stage-switch-btn-active': activeCreateStage === stage.key }"
                  @click="switchWorkflowStage(stage.key)"
                >
                  {{ stage.shortLabel }}
                </button>
              </div>
              <div class="workflow-summary__actions">
                <button class="btn-primary btn-sm" type="button" :disabled="busyActionKey === 'storyboard'" @click="handleGenerateStoryboard">
                  {{ busyActionKey === "storyboard" ? "生成中..." : "生成分镜版本" }}
                </button>
                <span class="surface-chip">{{ selectedWorkflow.storyboardVersions.length }} 个版本</span>
              </div>
            </div>
          </div>

          <section class="workflow-stage-content">
            <div v-if="!selectedWorkflow.storyboardVersions.length" class="workflow-empty">
              还没有分镜版本，先执行一次分镜生成。
            </div>
            <div v-else>
              <div v-if="selectedWorkflow.storyboardVersions.length > 1" class="workflow-scroll-hint">
                向下滚动选择分镜版本
              </div>
              <div class="version-grid storyboard-version-list">
                <article v-for="version in selectedWorkflow.storyboardVersions" :key="version.id" class="surface-tile version-card">
                  <div class="version-card__head">
                    <div>
                      <h3>{{ version.title }}</h3>
                      <div class="version-card__meta">
                        <span class="surface-chip">V{{ version.versionNo }}</span>
                        <span class="surface-chip">{{ version.status }}</span>
                        <span v-if="version.selected" class="surface-chip">当前选中</span>
                      </div>
                    </div>
                    <button class="btn-secondary btn-sm" type="button" :disabled="version.selected || busyActionKey === version.id" @click="handleSelectStoryboard(version.id)">
                      {{ busyActionKey === version.id ? "处理中..." : (version.selected ? "已选中" : "设为当前") }}
                    </button>
                  </div>

                  <div class="version-card__text version-card__markdown" v-html="storyboardPreviewHtml(version)"></div>

                  <div class="storyboard-adjust-row">
                    <input
                      v-model="storyboardAdjustmentDrafts[version.id]"
                      class="field-input storyboard-adjust-row__input"
                      type="text"
                      placeholder="输入调整要求，留空则自动审查"
                    />
                    <button
                      class="btn-primary btn-sm"
                      type="button"
                      :disabled="busyActionKey === `storyboard-adjust-${version.id}` || version.status !== 'SUCCEEDED'"
                      @click="handleAdjustStoryboard(version.id)"
                    >
                      {{ busyActionKey === `storyboard-adjust-${version.id}` ? "调整中..." : "调整" }}
                    </button>
                  </div>

                  <div class="rating-row">
                    <button
                      v-for="score in ratingOptions"
                      :key="`${version.id}-${score}`"
                      type="button"
                      class="rating-pill"
                      :class="{ 'rating-pill-active': Number(stageRatingDrafts[version.id] || version.rating || 0) === score }"
                      @click="setStageRatingDraft(version.id, score)"
                    >
                      {{ score }}
                    </button>
                  </div>
                  <textarea
                    v-model="stageNoteDrafts[version.id]"
                    class="field-textarea version-card__textarea"
                    rows="3"
                    placeholder="分镜评价"
                  ></textarea>

                  <div class="version-card__actions">
                    <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === `rate-${version.id}`" @click="handleRateStageVersion(version)">
                      {{ busyActionKey === `rate-${version.id}` ? "保存中..." : "保存评分" }}
                    </button>
                    <button
                      class="btn-ghost btn-sm"
                      type="button"
                      :disabled="!version.asset || busyActionKey === `reuse-${version.id}`"
                      @click="handleReuseAsset(version.asset?.id || '', version.id)"
                    >
                      {{ busyActionKey === `reuse-${version.id}` ? "复制中..." : "复制为新工作流" }}
                    </button>
                    <button class="btn-danger btn-sm" type="button" :disabled="busyActionKey === `delete-${version.id}`" @click="handleDeleteStageVersion(version)">
                      {{ busyActionKey === `delete-${version.id}` ? "删除中..." : "删除版本" }}
                    </button>
                  </div>

                </article>
              </div>
            </div>
          </section>
        </section>

        <section v-else-if="activeCreateStage === 'keyframe'" class="surface-panel workflow-panel workflow-stage-panel">
          <div class="workflow-panel__head">
            <div>
              <p class="workflow-eyebrow">Stage 2 / 3</p>
              <h2>关键帧模块</h2>
              <div class="workflow-summary__meta">
                <span class="surface-chip">{{ valueOptionLabel(imageModelOptions, selectedWorkflow.imageModel, selectedWorkflow.imageModel || "未设置") }}</span>
                <span class="surface-chip">{{ keyOptionLabel(stylePresetOptions, selectedWorkflow.stylePreset, selectedWorkflow.stylePreset || "未设置") }}</span>
                <span class="surface-chip">{{ selectedWorkflow.aspectRatio }}</span>
                <span class="surface-chip">关键帧 Seed {{ seedLabel(selectedWorkflow.keyframeSeed) }}</span>
              </div>
            </div>
            <div class="workflow-stage-panel__actions">
              <div class="stage-switch-row">
                <button
                  v-for="stage in createStageOptions"
                  :key="`detail-${stage.key}`"
                  type="button"
                  class="stage-switch-btn"
                  :class="{ 'stage-switch-btn-active': activeCreateStage === stage.key }"
                  @click="switchWorkflowStage(stage.key)"
                >
                  {{ stage.shortLabel }}
                </button>
              </div>
              <div class="workflow-summary__actions">
                <span class="surface-chip">{{ selectedWorkflow.clipSlots.length }} 个镜头</span>
              </div>
            </div>
          </div>

          <div v-if="!workflowCharacterSheets.length && !selectedWorkflow.clipSlots.length" class="workflow-empty">
            选中分镜版本后，这里会展开角色三视图与镜头列表。
          </div>

          <div v-else class="clip-stack">
            <section class="surface-tile clip-card clip-card-character-sheet-section">
              <div class="clip-card__head">
                <div>
                  <p class="workflow-eyebrow">Character Sheets</p>
                  <h3>角色三视图</h3>
                  <p class="clip-card__desc">首次进入关键帧阶段时，先为每个角色生成多版本三视图，再选中继续。</p>
                </div>
                <div class="clip-card__meta">
                  <span class="surface-chip">{{ workflowCharacterSheets.length }} 个角色</span>
                </div>
              </div>

              <div v-if="!workflowCharacterSheets.length" class="workflow-empty workflow-empty-nested">
                当前工作流还没有角色三视图。
              </div>
              <div v-else class="character-sheet-stack">
                <article v-for="sheet in workflowCharacterSheets" :key="characterSheetKey(sheet)" class="character-sheet-card">
                  <div class="clip-card__head">
                    <div>
                      <p class="workflow-eyebrow">Character {{ characterSheetClipIndex(sheet) ?? "-" }}</p>
                      <h3>{{ characterSheetTitle(sheet) }}</h3>
                      <p class="clip-card__desc">{{ characterSheetAppearanceSummary(sheet) }}</p>
                    </div>
                    <div class="clip-card__meta">
                      <span v-if="selectedCharacterSheetVersion(sheet)" class="surface-chip">
                        {{ selectedCharacterSheetVersion(sheet)?.title }}
                      </span>
                      <button
                        class="btn-primary btn-sm"
                        type="button"
                        :disabled="characterSheetClipIndex(sheet) === null"
                        @click="openCharacterAssetPicker(sheet)"
                      >
                        从素材库选择
                      </button>
                      <button
                        class="btn-ghost btn-sm"
                        type="button"
                        :disabled="characterSheetClipIndex(sheet) === null || busyActionKey === `keyframe-${characterSheetClipIndex(sheet)}`"
                        @click="handleGenerateKeyframe(characterSheetClipIndex(sheet) || 0)"
                      >
                        {{ busyActionKey === `keyframe-${characterSheetClipIndex(sheet)}` ? "生成中..." : "生成三视图" }}
                      </button>
                    </div>
                  </div>

                  <section v-if="isCharacterAssetPickerOpen(sheet)" class="character-asset-picker">
                    <div class="character-asset-picker__head">
                      <div>
                        <p class="workflow-eyebrow">Material Library</p>
                        <h4>选择 {{ characterSheetTitle(sheet) }} 的三视图素材</h4>
                      </div>
                      <button class="btn-secondary btn-sm" type="button" @click="closeCharacterAssetPicker">
                        收起
                      </button>
                    </div>

                    <div class="character-asset-picker__filters">
                      <label class="workflow-field">
                        <span>关键词</span>
                        <input
                          v-model="characterAssetPicker.keyword"
                          class="field-input"
                          type="search"
                          placeholder="按角色名、标题搜索"
                          @keyup.enter="loadCharacterAssetCandidates(sheet)"
                        />
                      </label>
                      <label class="workflow-field">
                        <span>模型</span>
                        <input
                          v-model="characterAssetPicker.model"
                          class="field-input"
                          type="search"
                          placeholder="按模型筛选"
                          @keyup.enter="loadCharacterAssetCandidates(sheet)"
                        />
                      </label>
                      <button
                        class="btn-secondary btn-sm character-asset-picker__search"
                        type="button"
                        :disabled="characterAssetPicker.loading"
                        @click="loadCharacterAssetCandidates(sheet)"
                      >
                        {{ characterAssetPicker.loading ? "搜索中..." : "搜索素材" }}
                      </button>
                    </div>

                    <p v-if="characterAssetPicker.error" class="workflow-error">{{ characterAssetPicker.error }}</p>
                    <div v-else-if="characterAssetPicker.loading" class="workflow-empty workflow-empty-nested">
                      正在加载三视图素材...
                    </div>
                    <div v-else-if="!characterAssetPicker.assets.length" class="workflow-empty workflow-empty-nested">
                      没有匹配的角色三视图素材。
                    </div>
                    <div v-else class="character-asset-grid">
                      <article v-for="asset in characterAssetPicker.assets" :key="asset.id" class="character-asset-card">
                        <button
                          type="button"
                          class="character-asset-card__preview"
                          :aria-label="`查看${asset.title}素材预览`"
                          @click="openImagePreview(materialAssetPreviewUrl(asset), asset.title)"
                        >
                          <img :src="materialAssetPreviewUrl(asset)" :alt="asset.title" />
                        </button>
                        <div class="character-asset-card__body">
                          <strong>{{ asset.title }}</strong>
                          <div class="character-asset-card__meta">
                            <span class="surface-chip surface-chip-quiet">{{ materialAssetModelLabel(asset) }}</span>
                            <span class="surface-chip surface-chip-quiet">评分 {{ ratingLabel(asset.userRating) }}</span>
                          </div>
                        </div>
                        <button
                          class="btn-primary btn-sm"
                          type="button"
                          :disabled="busyActionKey === `character-sheet-asset-${characterSheetClipIndex(sheet)}`"
                          @click="handleSelectCharacterSheetAsset(sheet, asset.id)"
                        >
                          {{ busyActionKey === `character-sheet-asset-${characterSheetClipIndex(sheet)}` ? "选择中..." : "选择此素材" }}
                        </button>
                      </article>
                    </div>
                  </section>

                  <div v-if="!characterSheetVersions(sheet).length" class="workflow-empty workflow-empty-nested">
                    还没有角色三视图版本。
                  </div>
                  <div v-else>
                    <div v-if="characterSheetVersions(sheet).length > 1" class="workflow-scroll-hint workflow-scroll-hint-nested">
                      左右滑动选择角色三视图版本
                    </div>
                    <div class="clip-version-list clip-version-list-grid">
                      <article v-for="version in characterSheetVersions(sheet)" :key="version.id" class="version-card version-card-compact">
                        <div class="version-card__head">
                          <div class="version-card__meta">
                            <strong>{{ version.title }}</strong>
                            <span class="surface-chip">角色三视图</span>
                            <span class="surface-chip">Seed {{ seedLabel(versionSeed(version) ?? selectedWorkflow.keyframeSeed) }}</span>
                            <span class="surface-chip" v-if="version.selected">当前选中</span>
                          </div>
                          <button
                            class="btn-secondary btn-sm"
                            type="button"
                            :disabled="version.selected || characterSheetClipIndex(sheet) === null || busyActionKey === version.id"
                            @click="handleSelectKeyframe(characterSheetClipIndex(sheet) || 0, version.id)"
                          >
                            {{ busyActionKey === version.id ? "处理中..." : "选中继续" }}
                          </button>
                        </div>

                        <div
                          v-if="characterSheetPreviewFrames(version).length"
                          class="keyframe-frame-grid character-sheet-frame-grid"
                          :class="{ 'character-sheet-frame-grid-single': characterSheetPreviewFrames(version).length === 1 }"
                        >
                          <article
                            v-for="frame in characterSheetPreviewFrames(version)"
                            :key="`${version.id}-${frame.role}`"
                            class="keyframe-frame-card keyframe-frame-card-compact"
                            :class="{ 'character-sheet-frame-card-single': characterSheetPreviewFrames(version).length === 1 }"
                          >
                            <div class="keyframe-frame-card__head">
                              <span class="surface-chip surface-chip-quiet">{{ frame.label }}</span>
                            </div>
                            <button
                              type="button"
                              class="character-sheet-preview-trigger"
                              :aria-label="`查看${characterSheetTitle(sheet)}${frame.label}原图`"
                              @click="openImagePreview(frame.url, `${characterSheetTitle(sheet)}${frame.label}`)"
                            >
                              <img
                                class="version-card__image keyframe-frame-card__image character-sheet-frame-card__image"
                                :class="{ 'character-sheet-frame-card__image-single': characterSheetPreviewFrames(version).length === 1 }"
                                :src="frame.url"
                                :alt="`${characterSheetTitle(sheet)}${frame.label}`"
                              />
                            </button>
                          </article>
                        </div>
                        <div v-else class="workflow-empty workflow-empty-nested">
                          当前版本还没有可预览的角色三视图。
                        </div>

                        <div class="rating-row">
                          <button
                            v-for="score in ratingOptions"
                            :key="`${version.id}-${score}`"
                            type="button"
                            class="rating-pill"
                            :class="{ 'rating-pill-active': Number(stageRatingDrafts[version.id] || version.rating || 0) === score }"
                            @click="setStageRatingDraft(version.id, score)"
                          >
                            {{ score }}
                          </button>
                        </div>

                        <textarea
                          v-model="stageNoteDrafts[version.id]"
                          class="field-textarea version-card__textarea"
                          rows="2"
                          placeholder="记录这组三视图的评价"
                        ></textarea>

                        <div class="version-card__actions">
                          <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === `rate-${version.id}`" @click="handleRateStageVersion(version)">
                            {{ busyActionKey === `rate-${version.id}` ? "保存中..." : "保存评分" }}
                          </button>
                          <button
                            class="btn-ghost btn-sm"
                            type="button"
                            :disabled="!version.asset || busyActionKey === `reuse-${version.id}`"
                            @click="handleReuseAsset(version.asset?.id || '', version.id)"
                          >
                            {{ busyActionKey === `reuse-${version.id}` ? "复制为新工作流" : "复用" }}
                          </button>
                          <button class="btn-danger btn-sm" type="button" :disabled="busyActionKey === `delete-${version.id}`" @click="handleDeleteStageVersion(version)">
                            {{ busyActionKey === `delete-${version.id}` ? "删除中..." : "删除版本" }}
                          </button>
                        </div>
                      </article>
                    </div>
                  </div>
                </article>
              </div>
            </section>

            <article v-for="slot in selectedWorkflow.clipSlots" :key="slot.clipIndex" class="surface-tile clip-card">
              <div class="clip-card__head">
                <div class="clip-card__main">
                  <p class="workflow-eyebrow">Clip {{ slot.clipIndex }}</p>
                  <div class="clip-card__title-row">
                    <h3>{{ slot.shotLabel || `镜头 #${slot.clipIndex}` }}</h3>
                    <HintBell
                      class="clip-card__bell"
                      title="完整分镜内容"
                      :text="clipSceneText(slot)"
                      :max-width="560"
                      align="left"
                    />
                  </div>
                  <p class="clip-card__desc">{{ clipSceneSummary(slot) }}</p>
                  <div v-if="slot.matchedCharacters?.length" class="clip-card__meta clip-card__meta-inline">
                    <span
                      v-for="character in slot.matchedCharacters"
                      :key="`${slot.clipIndex}-${character.clipIndex}-${character.characterName}`"
                      class="surface-chip surface-chip-quiet clip-card__character-chip"
                    >
                      {{ character.characterName }}
                    </span>
                  </div>
                </div>
                <div class="clip-card__meta clip-card__side-actions">
                  <span class="surface-chip clip-card__duration-chip">{{ slot.durationHint || `${slot.targetDurationSeconds || 0}s` }}</span>
                  <button class="btn-secondary btn-sm clip-card__action-button" type="button" :disabled="busyActionKey === `keyframe-${slot.clipIndex}`" @click="handleGenerateKeyframe(slot.clipIndex)">
                    {{ busyActionKey === `keyframe-${slot.clipIndex}` ? "生成中..." : "生成关键帧" }}
                  </button>
                </div>
              </div>

              <div v-if="!slot.keyframeVersions.length" class="workflow-empty workflow-empty-nested">
                还没有关键帧版本。
              </div>
              <div v-else>
                <div v-if="slot.keyframeVersions.length > 1" class="workflow-scroll-hint workflow-scroll-hint-nested">
                  左右滑动选择关键帧版本（含首帧与尾帧）
                </div>
                <div class="clip-version-list clip-version-list-grid">
                <article
                  v-for="version in slot.keyframeVersions"
                  :key="version.id"
                  class="version-card version-card-compact"
                  :class="{ 'version-card-keyframe-landscape': isLandscapeKeyframeVersion(version) }"
                >
                  <div class="version-card__head">
                    <div class="version-card__meta">
                      <strong>{{ version.title }}</strong>
                      <span class="surface-chip">首尾帧</span>
                      <span class="surface-chip">Seed {{ seedLabel(versionSeed(version) ?? selectedWorkflow.keyframeSeed) }}</span>
                      <span class="surface-chip" v-if="version.selected">当前选中</span>
                    </div>
                    <button class="btn-secondary btn-sm" type="button" :disabled="version.selected || busyActionKey === version.id" @click="handleSelectKeyframe(slot.clipIndex, version.id)">
                      {{ busyActionKey === version.id ? "处理中..." : "选中继续" }}
                    </button>
                  </div>

                  <div
                    v-if="keyframePreviewFrames(version, slot).length"
                    class="keyframe-frame-grid"
                    :class="{ 'keyframe-frame-grid-landscape': isLandscapeKeyframeVersion(version) }"
                  >
                    <article v-for="frame in keyframePreviewFrames(version, slot)" :key="`${version.id}-${frame.role}`" class="keyframe-frame-card">
                      <div class="keyframe-frame-card__head">
                        <span class="surface-chip surface-chip-quiet">{{ frame.label }}</span>
                        <span v-if="frame.selected" class="surface-chip">已选中</span>
                      </div>
                      <button
                        v-if="frame.url"
                        type="button"
                        class="character-sheet-preview-trigger"
                        :aria-label="`查看${version.title}${frame.label}原图`"
                        @click="openKeyframeImagePreview(version, frame)"
                      >
                        <img
                          class="version-card__image keyframe-frame-card__image"
                          :class="{ 'keyframe-frame-card__image-landscape': isLandscapeKeyframeVersion(version) }"
                          :src="frame.url"
                          :alt="`${version.title}${frame.label}`"
                        />
                      </button>
                      <div v-else class="keyframe-frame-card__failure">
                        <strong>{{ frame.label }}生成失败</strong>
                        <span>{{ frame.errorMessage || "请单独重生此帧" }}</span>
                      </div>
                      <div class="keyframe-frame-card__actions">
                        <button
                          class="btn-secondary btn-sm"
                          type="button"
                          :disabled="frame.selected || busyActionKey === `${version.id}-${frame.role}`"
                          @click="handleSelectKeyframeFrame(slot.clipIndex, version.id, frame.role)"
                        >
                          {{ busyActionKey === `${version.id}-${frame.role}` ? "处理中..." : "选中此帧" }}
                        </button>
                        <button
                          class="btn-ghost btn-sm"
                          type="button"
                          :disabled="!frame.regenerable || busyActionKey === `keyframe-${slot.clipIndex}-${frame.role}`"
                          :title="frame.regenerable ? `单独重生${frame.label}` : '该首帧由上一镜头尾帧承接，请重生上一镜头尾帧'"
                          @click="handleGenerateKeyframeFrame(slot.clipIndex, frame.role)"
                        >
                          {{ busyActionKey === `keyframe-${slot.clipIndex}-${frame.role}` ? "生成中..." : "重生此帧" }}
                        </button>
                      </div>
                    </article>
                  </div>
                  <div v-else class="workflow-empty workflow-empty-nested">
                    当前版本还没有可预览的首尾帧。
                  </div>

                  <div class="rating-row">
                    <button
                      v-for="score in ratingOptions"
                      :key="`${version.id}-${score}`"
                      type="button"
                      class="rating-pill"
                      :class="{ 'rating-pill-active': Number(stageRatingDrafts[version.id] || version.rating || 0) === score }"
                      @click="setStageRatingDraft(version.id, score)"
                    >
                      {{ score }}
                    </button>
                  </div>

                  <textarea
                    v-model="stageNoteDrafts[version.id]"
                    class="field-textarea version-card__textarea"
                    rows="2"
                    placeholder="记录这组首尾帧的评价"
                  ></textarea>

                  <div class="version-card__actions">
                    <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === `rate-${version.id}`" @click="handleRateStageVersion(version)">
                      {{ busyActionKey === `rate-${version.id}` ? "保存中..." : "保存评分" }}
                    </button>
                    <button
                      class="btn-ghost btn-sm"
                      type="button"
                      :disabled="!version.asset || busyActionKey === `reuse-${version.id}`"
                      @click="handleReuseAsset(version.asset?.id || '', version.id)"
                    >
                      {{ busyActionKey === `reuse-${version.id}` ? "复制为新工作流" : "复用" }}
                    </button>
                    <button class="btn-danger btn-sm" type="button" :disabled="busyActionKey === `delete-${version.id}`" @click="handleDeleteStageVersion(version)">
                      {{ busyActionKey === `delete-${version.id}` ? "删除中..." : "删除版本" }}
                    </button>
                  </div>
                </article>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section v-else class="surface-panel workflow-panel workflow-stage-panel">
          <div class="workflow-panel__head">
            <div>
              <p class="workflow-eyebrow">Stage 3 / Finalize</p>
              <h2>视频生成模块</h2>
              <div class="workflow-summary__meta">
                <span class="surface-chip">{{ valueOptionLabel(videoModelOptions, selectedWorkflow.videoModel, selectedWorkflow.videoModel || "未设置") }}</span>
                <span class="surface-chip">{{ valueOptionLabel(catalogVideoSizeOptions, selectedWorkflow.videoSize, selectedWorkflow.videoSize || "未设置") }}</span>
                <span class="surface-chip">视频 Seed {{ seedLabel(selectedWorkflow.videoSeed) }}</span>
              </div>
            </div>
            <div class="workflow-stage-panel__actions">
              <div class="stage-switch-row">
                <button
                  v-for="stage in createStageOptions"
                  :key="`detail-${stage.key}`"
                  type="button"
                  class="stage-switch-btn"
                  :class="{ 'stage-switch-btn-active': activeCreateStage === stage.key }"
                  @click="switchWorkflowStage(stage.key)"
                >
                  {{ stage.shortLabel }}
                </button>
              </div>
              <div class="workflow-summary__actions">
                <span class="surface-chip">{{ selectedWorkflow.clipSlots.length }} 个镜头</span>
              </div>
            </div>
          </div>

          <div class="workflow-stage-grid">
            <section class="surface-tile workflow-summary-card workflow-summary-card-compact">
              <h3>最终结果与评分</h3>
              <div class="rating-row">
                <button
                  v-for="score in ratingOptions"
                  :key="score"
                  type="button"
                  class="rating-pill"
                  :class="{ 'rating-pill-active': Number(workflowRatingDraft) === score }"
                  @click="workflowRatingDraft = String(score)"
                >
                  {{ score }}
                </button>
              </div>
              <textarea
                v-model="workflowRatingNoteDraft"
                class="field-textarea"
                rows="4"
                placeholder="记录最终成片评价"
              ></textarea>
              <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === 'workflow-rating'" @click="handleRateWorkflow">
                {{ busyActionKey === "workflow-rating" ? "保存中..." : "保存评分" }}
              </button>

              <div class="workflow-finalize-box">
                <button
                  class="btn-primary btn-sm workflow-finalize-box__button"
                  type="button"
                  :disabled="!canFinalize || busyActionKey === 'finalize'"
                  @click="handleFinalize"
                >
                  {{ busyActionKey === "finalize" ? "拼接中..." : finalizeButtonLabel }}
                </button>
                <p class="workflow-finalize-box__hint">
                  {{ finalizeHint }}
                </p>
              </div>

              <div v-if="selectedWorkflow.finalResult" class="workflow-final-compact">
                <video
                  v-if="selectedWorkflow.finalResult.previewUrl"
                  class="final-result-card__video"
                  :src="selectedWorkflow.finalResult.previewUrl"
                  controls
                  playsinline
                  preload="metadata"
                ></video>
                <div class="workflow-kv">
                  <div class="workflow-kv__row">
                    <span>标题</span>
                    <strong>{{ selectedWorkflow.finalResult.title }}</strong>
                  </div>
                  <div class="workflow-kv__row">
                    <span>时长</span>
                    <strong>{{ durationLabel(selectedWorkflow.finalResult.durationSeconds) }}</strong>
                  </div>
                </div>
                <div class="version-card__actions">
                  <a
                    class="btn-primary btn-sm"
                    :href="selectedWorkflow.finalResult.fileUrl"
                    download
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    下载结果视频
                  </a>
                </div>
              </div>
            </section>

            <section class="workflow-stage-content">
              <div v-if="!selectedWorkflow.clipSlots.length" class="workflow-empty">
                选中分镜版本后，这里会展开视频生成列表。
              </div>
              <div v-else class="clip-stack">
                <article v-for="slot in selectedWorkflow.clipSlots" :key="`video-${slot.clipIndex}`" class="surface-tile clip-card">
                  <div class="clip-card__head">
                    <div class="clip-card__main">
                      <p class="workflow-eyebrow">Clip {{ slot.clipIndex }}</p>
                      <div class="clip-card__title-row">
                        <h3>{{ slot.shotLabel || `镜头 #${slot.clipIndex}` }}</h3>
                        <HintBell
                          class="clip-card__bell"
                          title="完整分镜内容"
                          :text="clipSceneText(slot)"
                          :max-width="560"
                          align="left"
                        />
                      </div>
                      <p class="clip-card__desc">{{ clipSceneSummary(slot) }}</p>
                    </div>
                    <div class="clip-card__meta clip-card__side-actions clip-card__side-actions-video">
                      <span class="surface-chip clip-card__selection-chip">
                        {{ selectedKeyframeVersion(slot)?.title || "未选择关键帧" }}
                      </span>
                      <button
                        class="btn-primary btn-sm clip-card__action-button"
                        type="button"
                        :disabled="!selectedKeyframeVersion(slot) || busyActionKey === `video-${slot.clipIndex}`"
                        @click="handleGenerateVideo(slot.clipIndex)"
                      >
                        {{ busyActionKey === `video-${slot.clipIndex}` ? "生成中..." : "生成视频" }}
                      </button>
                    </div>
                  </div>

                  <div v-if="selectedKeyframeVersion(slot) && selectedKeyframePreviewFrames(slot).length" class="clip-card__selected-keyframes">
                    <div class="clip-card__selected-keyframes-head">
                      <strong>当前视频输入帧</strong>
                      <span class="surface-chip surface-chip-quiet">{{ selectedKeyframeVersion(slot)?.title }}</span>
                    </div>
                    <div class="keyframe-thumb-list">
                      <article
                        v-for="frame in selectedKeyframePreviewFrames(slot)"
                        :key="`selected-${slot.clipIndex}-${frame.role}`"
                        class="keyframe-thumb-card"
                      >
                        <img class="keyframe-thumb-card__image" :src="frame.url" :alt="`${slot.shotLabel || `镜头 #${slot.clipIndex}`}${frame.label}`" />
                        <div class="keyframe-thumb-card__meta">
                          <span class="surface-chip surface-chip-quiet">{{ frame.label }}</span>
                        </div>
                      </article>
                    </div>
                  </div>

                  <div v-if="!slot.videoVersions.length" class="workflow-empty workflow-empty-nested">
                    还没有视频版本。
                  </div>
                  <div v-else>
                    <div v-if="slot.videoVersions.length > 1" class="workflow-scroll-hint workflow-scroll-hint-nested">
                      左右滑动选择视频版本
                    </div>
                    <div class="clip-version-list clip-version-list-grid">
                    <article v-for="version in slot.videoVersions" :key="version.id" class="version-card version-card-compact">
                      <div class="version-card__head">
                        <div class="version-card__meta">
                          <strong>{{ version.title }}</strong>
                          <span class="surface-chip">Seed {{ seedLabel(versionSeed(version) ?? selectedWorkflow.videoSeed) }}</span>
                          <span class="surface-chip" v-if="version.selected">当前选中</span>
                        </div>
                        <button class="btn-secondary btn-sm" type="button" :disabled="version.selected || busyActionKey === version.id" @click="handleSelectVideo(slot.clipIndex, version.id)">
                          {{ busyActionKey === version.id ? "处理中..." : "选中继续" }}
                        </button>
                      </div>

                      <video v-if="version.previewUrl" class="version-card__video" :src="version.previewUrl" controls playsinline preload="metadata"></video>

                      <div class="rating-row">
                        <button
                          v-for="score in ratingOptions"
                          :key="`${version.id}-${score}`"
                          type="button"
                          class="rating-pill"
                          :class="{ 'rating-pill-active': Number(stageRatingDrafts[version.id] || version.rating || 0) === score }"
                          @click="setStageRatingDraft(version.id, score)"
                        >
                          {{ score }}
                        </button>
                      </div>

                      <textarea
                        v-model="stageNoteDrafts[version.id]"
                        class="field-textarea version-card__textarea"
                        rows="2"
                        placeholder="视频评价"
                      ></textarea>

                      <div class="version-card__actions">
                        <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === `rate-${version.id}`" @click="handleRateStageVersion(version)">
                          {{ busyActionKey === `rate-${version.id}` ? "保存中..." : "保存评分" }}
                        </button>
                        <button
                          class="btn-ghost btn-sm"
                          type="button"
                          :disabled="!version.asset || busyActionKey === `reuse-${version.id}`"
                          @click="handleReuseAsset(version.asset?.id || '', version.id)"
                        >
                          {{ busyActionKey === `reuse-${version.id}` ? "复制为新工作流" : "复用" }}
                        </button>
                        <a
                          v-if="version.downloadUrl"
                          class="btn-ghost btn-sm"
                          :href="version.downloadUrl"
                          download
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          下载
                        </a>
                        <button class="btn-danger btn-sm" type="button" :disabled="busyActionKey === `delete-${version.id}`" @click="handleDeleteStageVersion(version)">
                          {{ busyActionKey === `delete-${version.id}` ? "删除中..." : "删除版本" }}
                        </button>
                      </div>
                    </article>
                    </div>
                  </div>
                </article>
              </div>
            </section>
          </div>
        </section>
      </template>

      <section v-else-if="!createComposerVisible" class="surface-panel workflow-panel workflow-empty-large workflow-empty-prompt">
        <h2>请新建工作流或选择一个工作流来查看</h2>
        <div class="workflow-empty-prompt__actions">
          <button class="btn-primary btn-sm" type="button" :disabled="creatingWorkflow || loadingOptions" @click="startCreateWorkflow">
            新建工作流
          </button>
        </div>
      </section>
    </section>
  </section>

  <div v-if="imagePreviewState.open" class="image-preview-overlay" role="dialog" aria-modal="true" @click.self="closeImagePreview">
    <div class="image-preview-caption">
      <strong>{{ imagePreviewCaption }}</strong>
      <span v-if="imagePreviewState.gallery.length > 1">按 ← / → 切换首尾帧</span>
    </div>
    <button type="button" class="image-preview-close" aria-label="关闭原图预览" @click="closeImagePreview">
      关闭
    </button>
    <img class="image-preview-full" :src="imagePreviewState.url" :alt="imagePreviewState.alt" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchGenerationOptions } from "@/api/generation";
import { fetchMaterialAssets, reuseMaterialAsset } from "@/api/material-assets";
import {
  adjustStoryboard,
  createWorkflow,
  deleteStageVersion,
  deleteWorkflow,
  fetchWorkflow,
  fetchWorkflows,
  finalizeWorkflow,
  generateKeyframe,
  generateKeyframeFrame,
  generateStoryboard,
  generateVideo,
  rateStageVersion,
  rateWorkflow,
  selectCharacterSheetAsset,
  selectKeyframe,
  selectKeyframeFrame,
  selectStoryboard,
  selectVideo,
  updateWorkflowSettings,
} from "@/api/workflows";
import HintBell from "@/components/HintBell.vue";
import { renderMarkdownToHtml } from "@/utils/markdown";
import type {
  CreateWorkflowRequest,
  GenerationOptionsResponse,
  GenerationVideoSizeOption,
  MaterialAssetLibraryItem,
  StageVersion,
  UpdateWorkflowSettingsRequest,
  WorkflowCharacterSheet,
  WorkflowClipSlot,
  WorkflowDeleteResult,
  WorkflowDetail,
  WorkflowSummary,
} from "@/types";

type CreateStageKey = "storyboard" | "keyframe" | "video";

interface StageFieldStep {
  key: string;
  label: string;
  description: string;
  valueLabel: string;
}

interface CreateReviewItem {
  key: string;
  label: string;
  valueLabel: string;
  configured: boolean;
  required: boolean;
}

interface CreateReviewSection {
  key: string;
  title: string;
  eyebrow: string;
  items: CreateReviewItem[];
}

interface PreviewFrame {
  role: string;
  label: string;
  url: string;
  selected?: boolean;
  regenerable?: boolean;
  errorMessage?: string;
}

interface ImagePreviewItem {
  url: string;
  alt: string;
  caption: string;
}

const router = useRouter();
const route = useRoute();

const loadingOptions = ref(false);
const options = ref<GenerationOptionsResponse | null>(null);

const loadingWorkflows = ref(false);
const loadingDetail = ref(false);
const creatingWorkflow = ref(false);
const busyActionKey = ref("");
const workflowSearch = ref("");
const activeCreateStage = ref<CreateStageKey>("storyboard");
const createReviewFlipped = ref(false);
const createComposerVisible = ref(false);
const stageFieldIndexMap = reactive<Record<CreateStageKey, number>>({
  storyboard: 0,
  keyframe: 0,
  video: 0,
});

const listError = ref("");
const detailError = ref("");
const createError = ref("");
const characterAssetPicker = reactive({
  openKey: "",
  keyword: "",
  model: "",
  loading: false,
  error: "",
  assets: [] as MaterialAssetLibraryItem[],
});
const imagePreviewState = reactive({
  open: false,
  url: "",
  alt: "",
  caption: "",
  gallery: [] as ImagePreviewItem[],
  currentIndex: 0,
});

const workflows = ref<WorkflowSummary[]>([]);
const selectedWorkflow = ref<WorkflowDetail | null>(null);

const createStageOptions = [
  {
    key: "storyboard" as const,
    label: "文本分镜卡片",
    shortLabel: "分镜脚本",
    description: "文本模型、镜头时长",
  },
  {
    key: "keyframe" as const,
    label: "关键帧卡片",
    shortLabel: "关键帧",
    description: "图片模型、风格、关键帧 Seed",
  },
  {
    key: "video" as const,
    label: "视频生成卡片",
    shortLabel: "视频生成",
    description: "视频模型、视频 Seed",
  },
];
const detailStageKeys: CreateStageKey[] = ["storyboard", "keyframe", "video"];

const workflowRatingDraft = ref("5");
const workflowRatingNoteDraft = ref("");
const stageRatingDrafts = reactive<Record<string, string>>({});
const stageNoteDrafts = reactive<Record<string, string>>({});
const storyboardAdjustmentDrafts = reactive<Record<string, string>>({});
const workflowSettingsOpen = ref(false);
const workflowSettingsDraft = reactive({
  aspectRatio: "9:16",
  stylePreset: "",
  textAnalysisModel: "",
  imageModel: "",
  videoModel: "",
  videoSize: "",
  keyframeSeed: "",
  videoSeed: "",
  durationMode: "auto" as "auto" | "manual",
  minDurationSeconds: "5",
  maxDurationSeconds: "12",
});
const storyboardDurationMode = ref<"auto" | "manual">("auto");
const storyboardManualDurationSeconds = ref("8");
const STORYBOARD_MANUAL_DURATION_MIN_SECONDS = 5;
const STORYBOARD_MANUAL_DURATION_MAX_SECONDS = 12;

const createForm = reactive({
  title: "",
  transcriptText: "",
  globalPrompt: "",
  aspectRatio: "9:16",
  stylePreset: "",
  textAnalysisModel: "",
  imageModel: "",
  videoModel: "",
  videoSize: "",
  keyframeSeed: "",
  videoSeed: "",
});

const ratingOptions = [5, 4, 3, 2, 1];

const selectedWorkflowId = computed(() => {
  const workflowId = route.params.workflowId;
  return typeof workflowId === "string" ? workflowId : "";
});

function normalizeDetailStage(value: unknown): CreateStageKey | null {
  const rawValue = Array.isArray(value) ? value[0] : value;
  if (typeof rawValue !== "string") {
    return null;
  }
  const normalizedValue = rawValue.trim().toLowerCase();
  if (normalizedValue === "joined") {
    return "video";
  }
  return detailStageKeys.includes(normalizedValue as CreateStageKey) ? normalizedValue as CreateStageKey : null;
}

const workflowCharacterSheets = computed(() => selectedWorkflow.value?.characterSheets ?? []);
const imagePreviewCaption = computed(() => imagePreviewState.caption || imagePreviewState.alt || "图片预览");

const filteredWorkflows = computed(() => {
  const keyword = workflowSearch.value.trim().toLowerCase();
  if (!keyword) {
    return workflows.value;
  }
  return workflows.value.filter((item) => {
    const haystack = [
      item.title,
      item.status,
      item.currentStage,
      item.aspectRatio,
    ].join(" ").toLowerCase();
    return haystack.includes(keyword);
  });
});

const activeCreateStageMeta = computed(() =>
  createStageOptions.find((item) => item.key === activeCreateStage.value) ?? createStageOptions[0]
);

const aspectRatioOptions = computed(() => options.value?.aspectRatios ?? [
  { value: "9:16", label: "9:16" },
  { value: "16:9", label: "16:9" },
]);
const stylePresetOptions = computed(() => options.value?.stylePresets ?? []);
const textModelOptions = computed(() => options.value?.textAnalysisModels ?? []);
const imageModelOptions = computed(() => options.value?.imageModels ?? []);
const videoModelOptions = computed(() => options.value?.videoModels ?? []);
const catalogVideoSizeOptions = computed(() => options.value?.videoSizes ?? []);
const videoSizeOptions = computed(() =>
  filterVideoSizeOptions(catalogVideoSizeOptions.value, createForm.videoModel, createForm.aspectRatio)
);
const workflowSettingsVideoSizeOptions = computed(() =>
  filterVideoSizeOptions(catalogVideoSizeOptions.value, workflowSettingsDraft.videoModel, workflowSettingsDraft.aspectRatio)
);

const canFinalize = computed(() => {
  const workflow = selectedWorkflow.value;
  if (!workflow || !workflow.clipSlots.length) {
    return false;
  }
  return workflow.clipSlots.every((slot) => slot.videoVersions.some((version) => version.selected));
});

const finalizeButtonLabel = computed(() => selectedWorkflow.value?.finalResult ? "重新拼接完整视频" : "拼接完整视频");

const finalizeHint = computed(() => {
  const workflow = selectedWorkflow.value;
  if (!workflow || !workflow.clipSlots.length) {
    return "先生成并选中每个分镜的视频版本，才能按分镜顺序拼接成完整视频。";
  }
  if (canFinalize.value) {
    return "会按当前选中的视频版本，依照分镜顺序拼接为完整视频。";
  }
  return "所有分镜都需要先有一个选中的视频版本，之后才能进行拼接。";
});

const workflowSettingsValidationMessage = computed(() => {
  if (!workflowSettingsDraft.textAnalysisModel) {
    return "请选择文本模型";
  }
  if (!workflowSettingsDraft.imageModel) {
    return "请选择关键帧模型";
  }
  if (!workflowSettingsDraft.stylePreset) {
    return "请选择视觉风格";
  }
  if (!workflowSettingsDraft.aspectRatio) {
    return "请选择长宽比";
  }
  if (!workflowSettingsDraft.videoModel) {
    return "请选择视频模型";
  }
  if (!workflowSettingsDraft.videoSize) {
    return "请选择输出尺寸";
  }
  if (workflowSettingsDraft.durationMode === "auto") {
    return "";
  }
  const minDuration = optionalInteger(workflowSettingsDraft.minDurationSeconds);
  const maxDuration = optionalInteger(workflowSettingsDraft.maxDurationSeconds);
  if (minDuration === null || maxDuration === null || minDuration < 1 || maxDuration < 1) {
    return "请填写合法的镜头时长";
  }
  if (maxDuration < minDuration) {
    return "最大时长不能小于最小时长";
  }
  return "";
});

const activeStageSteps = computed<StageFieldStep[]>(() => {
  switch (activeCreateStage.value) {
    case "storyboard":
      return [
        {
          key: "textAnalysisModel",
          label: "文本模型",
          description: "选择这一轮文本分镜所使用的分析模型。",
          valueLabel: valueOptionLabel(textModelOptions.value, createForm.textAnalysisModel, "未设置"),
        },
        {
          key: "storyboardDurationSeconds",
          label: "镜头时长",
          description: "统一设置镜头时长，可切换自动或手动固定时长。",
          valueLabel: storyboardDurationMode.value === "auto"
            ? "自动"
            : (normalizedStoryboardManualDurationSeconds.value === null ? "未设置" : `${normalizedStoryboardManualDurationSeconds.value} 秒`),
        },
      ];
    case "keyframe":
      return [
        {
          key: "imageModel",
          label: "关键帧模型",
          description: "选择生成关键帧的图片模型。",
          valueLabel: valueOptionLabel(imageModelOptions.value, createForm.imageModel, "未设置"),
        },
        {
          key: "stylePreset",
          label: "风格预设",
          description: "控制关键帧整体画风和镜头气质。",
          valueLabel: keyOptionLabel(stylePresetOptions.value, createForm.stylePreset, "未设置"),
        },
        {
          key: "aspectRatio",
          label: "长宽比",
          description: "决定关键帧与后续视频的画面比例。",
          valueLabel: valueOptionLabel(aspectRatioOptions.value, createForm.aspectRatio, "未设置"),
        },
        {
          key: "keyframeSeed",
          label: "关键帧 Seed",
          description: "可选，固定关键帧阶段的随机种子，便于稳定复现画面。",
          valueLabel: createForm.keyframeSeed === "" ? "自动" : createForm.keyframeSeed,
        },
      ];
    default:
      return [
        {
          key: "videoModel",
          label: "视频模型",
          description: "选择最终视频片段的生成模型。",
          valueLabel: valueOptionLabel(videoModelOptions.value, createForm.videoModel, "未设置"),
        },
        {
          key: "videoSize",
          label: "输出尺寸",
          description: "控制视频输出分辨率与尺寸基线。",
          valueLabel: valueOptionLabel(videoSizeOptions.value, createForm.videoSize, "未设置"),
        },
        {
          key: "videoSeed",
          label: "视频 Seed",
          description: "可选，固定视频阶段的随机种子，避免与关键帧阶段互相干扰。",
          valueLabel: createForm.videoSeed === "" ? "自动" : createForm.videoSeed,
        },
      ];
  }
});

const activeStageFieldIndex = computed(() => {
  const maxIndex = Math.max(activeStageSteps.value.length - 1, 0);
  return Math.min(stageFieldIndexMap[activeCreateStage.value] ?? 0, maxIndex);
});

const currentStageField = computed(
  () => activeStageSteps.value[activeStageFieldIndex.value] ?? activeStageSteps.value[0] ?? null
);

const activeStageProgress = computed(() => {
  if (!activeStageSteps.value.length) {
    return 0;
  }
  return ((activeStageFieldIndex.value + 1) / activeStageSteps.value.length) * 100;
});

const createReviewSections = computed<CreateReviewSection[]>(() => [
  {
    key: "base",
    eyebrow: "Workflow Base",
    title: "基础信息",
    items: [
      {
        key: "title",
        label: "标题",
        valueLabel: createForm.title.trim() || "未填写",
        configured: Boolean(createForm.title.trim()),
        required: true,
      },
      {
        key: "transcriptText",
        label: "正文",
        valueLabel: createForm.transcriptText.trim() ? "已填写" : "未填写",
        configured: Boolean(createForm.transcriptText.trim()),
        required: true,
      },
      {
        key: "globalPrompt",
        label: "全局 Prompt",
        valueLabel: createForm.globalPrompt.trim() ? "已填写" : "未填写",
        configured: Boolean(createForm.globalPrompt.trim()),
        required: false,
      },
    ],
  },
  {
    key: "storyboard",
    eyebrow: "Stage 1",
    title: "文本分镜",
    items: [
      {
        key: "textAnalysisModel",
        label: "文本模型",
        valueLabel: valueOptionLabel(textModelOptions.value, createForm.textAnalysisModel, "未设置"),
        configured: Boolean(createForm.textAnalysisModel),
        required: true,
      },
      {
        key: "storyboardDurationSeconds",
        label: "镜头时长",
        valueLabel: storyboardDurationMode.value === "auto"
          ? "自动"
          : (normalizedStoryboardManualDurationSeconds.value === null ? "未设置" : `${normalizedStoryboardManualDurationSeconds.value} 秒`),
        configured: isStoryboardDurationConfigured.value,
        required: true,
      },
    ],
  },
  {
    key: "keyframe",
    eyebrow: "Stage 2",
    title: "关键帧",
    items: [
      {
        key: "imageModel",
        label: "关键帧模型",
        valueLabel: valueOptionLabel(imageModelOptions.value, createForm.imageModel, "未设置"),
        configured: Boolean(createForm.imageModel),
        required: true,
      },
      {
        key: "stylePreset",
        label: "风格预设",
        valueLabel: keyOptionLabel(stylePresetOptions.value, createForm.stylePreset, "未设置"),
        configured: Boolean(createForm.stylePreset),
        required: true,
      },
      {
        key: "aspectRatio",
        label: "长宽比",
        valueLabel: valueOptionLabel(aspectRatioOptions.value, createForm.aspectRatio, "未设置"),
        configured: Boolean(createForm.aspectRatio),
        required: true,
      },
      {
        key: "keyframeSeed",
        label: "关键帧 Seed",
        valueLabel: createForm.keyframeSeed === "" ? "自动" : createForm.keyframeSeed,
        configured: true,
        required: false,
      },
    ],
  },
  {
    key: "video",
    eyebrow: "Stage 3",
    title: "视频生成",
    items: [
      {
        key: "videoModel",
        label: "视频模型",
        valueLabel: valueOptionLabel(videoModelOptions.value, createForm.videoModel, "未设置"),
        configured: Boolean(createForm.videoModel),
        required: true,
      },
      {
        key: "videoSize",
        label: "输出尺寸",
        valueLabel: valueOptionLabel(videoSizeOptions.value, createForm.videoSize, "未设置"),
        configured: Boolean(createForm.videoSize),
        required: true,
      },
      {
        key: "videoSeed",
        label: "视频 Seed",
        valueLabel: createForm.videoSeed === "" ? "自动" : createForm.videoSeed,
        configured: true,
        required: false,
      },
    ],
  },
]);

const createReviewRequiredItems = computed(() =>
  createReviewSections.value.flatMap((section) => section.items.filter((item) => item.required))
);

const createReviewConfiguredCount = computed(() =>
  createReviewRequiredItems.value.filter((item) => item.configured).length
);

const canSubmitCreateReview = computed(() =>
  createReviewRequiredItems.value.every((item) => item.configured)
);

function ratingLabel(value?: number | null) {
  return typeof value === "number" && value > 0 ? `${value}/5` : "未评分";
}

function valueOptionLabel<T extends { value: string; label: string }>(options: T[], value?: string | null, fallback = "-") {
  if (!value) {
    return fallback;
  }
  return options.find((item) => item.value === value)?.label || value;
}

function keyOptionLabel<T extends { key: string; label: string }>(options: T[], value?: string | null, fallback = "-") {
  if (!value) {
    return fallback;
  }
  return options.find((item) => item.key === value)?.label || value;
}

function normalizeModelName(value?: string | null) {
  return String(value ?? "").trim().toLowerCase();
}

function resolveVideoSizeAspectRatio(size: GenerationVideoSizeOption): "9:16" | "16:9" | null {
  const width = Number(size.width ?? 0);
  const height = Number(size.height ?? 0);
  if (width > 0 && height > 0) {
    return width > height ? "16:9" : "9:16";
  }
  const normalized = String(size.value ?? "").replace(/\*/g, "x").toLowerCase();
  const [rawWidth, rawHeight] = normalized.split("x");
  const parsedWidth = Number(rawWidth);
  const parsedHeight = Number(rawHeight);
  if (parsedWidth > 0 && parsedHeight > 0) {
    return parsedWidth > parsedHeight ? "16:9" : "9:16";
  }
  return null;
}

function compareVideoSizeByArea(a: GenerationVideoSizeOption, b: GenerationVideoSizeOption) {
  const areaA = Number(a.width ?? 0) * Number(a.height ?? 0);
  const areaB = Number(b.width ?? 0) * Number(b.height ?? 0);
  return areaA - areaB;
}

function filterVideoSizeOptions(source: GenerationVideoSizeOption[], model: string, aspectRatio: string) {
  const selectedModel = normalizeModelName(model);
  const filtered = source
    .filter((item) => {
      const itemAspectRatio = resolveVideoSizeAspectRatio(item);
      return !itemAspectRatio || itemAspectRatio === aspectRatio;
    })
    .filter((item) => {
      if (!selectedModel) {
        return true;
      }
      const supportedModels = Array.isArray(item.supportedModels) ? item.supportedModels : [];
      if (!supportedModels.length) {
        return true;
      }
      return supportedModels.some((itemModel) => normalizeModelName(itemModel) === selectedModel);
    });
  return [...filtered].sort(compareVideoSizeByArea);
}

function syncVideoSizeSelection(target: { videoSize: string; videoModel: string; aspectRatio: string }, preferred?: string | null) {
  if (!catalogVideoSizeOptions.value.length) {
    return;
  }
  const available = filterVideoSizeOptions(catalogVideoSizeOptions.value, target.videoModel, target.aspectRatio);
  if (!available.length) {
    target.videoSize = "";
    return;
  }
  const preferredValue = preferred || "";
  const next =
    available.find((item) => item.value === preferredValue)?.value
    ?? available.find((item) => item.value === target.videoSize)?.value
    ?? available[0].value;
  target.videoSize = next;
}

function parseStoryboardDurationSeconds(value?: string | null) {
  if (value === undefined || value === null) {
    return null;
  }
  const raw = String(value).trim();
  if (!raw) {
    return null;
  }
  const numericValue = Number(raw);
  if (!Number.isFinite(numericValue) || !Number.isInteger(numericValue)) {
    return null;
  }
  return Math.trunc(numericValue);
}

function seedLabel(value?: number | string | null) {
  if (value === undefined || value === null || value === "") {
    return "自动";
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? String(numericValue) : "自动";
}

function optionalInteger(value?: string | number | null) {
  if (value === undefined || value === null || value === "") {
    return null;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) && Number.isInteger(numericValue) ? numericValue : null;
}

function versionSeed(version: StageVersion) {
  const inputSummary = version.inputSummary ?? {};
  const seed = inputSummary.seed;
  if (seed === undefined || seed === null || seed === "") {
    return null;
  }
  const numericValue = Number(seed);
  return Number.isFinite(numericValue) ? numericValue : null;
}

function durationLabel(value?: number | null) {
  return typeof value === "number" && Number.isFinite(value) && value > 0 ? `${value.toFixed(1)}s` : "-";
}

function clipSceneText(slot: WorkflowClipSlot) {
  return (slot.scene?.trim() || "暂无场景描述");
}

function clipSceneSummary(slot: WorkflowClipSlot) {
  const text = clipSceneText(slot);
  return text.length > 120 ? `${text.slice(0, 120)}...` : text;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function storyboardPreview(version: StageVersion) {
  const outputSummary = version.outputSummary ?? {};
  const scriptMarkdown = typeof outputSummary.scriptMarkdown === "string" ? outputSummary.scriptMarkdown : "";
  const previewText = typeof outputSummary.previewText === "string" ? outputSummary.previewText : "";
  return scriptMarkdown || previewText || "暂无分镜预览";
}

function storyboardPreviewHtml(version: StageVersion) {
  return renderMarkdownToHtml(storyboardPreview(version));
}

function summaryUrlValue(summary: Record<string, unknown> | null | undefined, ...keys: string[]) {
  for (const key of keys) {
    const value = summary?.[key];
    if (typeof value === "string" && value.trim()) {
      return value;
    }
  }
  return "";
}

function summaryUrlListValue(summary: Record<string, unknown> | null | undefined, ...keys: string[]) {
  for (const key of keys) {
    const value = summary?.[key];
    if (Array.isArray(value)) {
      const urls = value.filter((item): item is string => typeof item === "string" && Boolean(item.trim()));
      if (urls.length) {
        return urls;
      }
    }
  }
  return [];
}

function summaryNumberValue(summary: Record<string, unknown> | null | undefined, ...keys: string[]) {
  for (const key of keys) {
    const value = summary?.[key];
    const numericValue = Number(value);
    if (Number.isFinite(numericValue) && numericValue > 0) {
      return numericValue;
    }
  }
  return 0;
}

function summaryFrameFailures(summary: Record<string, unknown> | null | undefined) {
  const value = summary?.frameFailures;
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === "object" && !Array.isArray(item))
    .map((item) => ({
      role: typeof item.frameRole === "string" && item.frameRole.trim() === "last" ? "last" : "first",
      message: typeof item.errorMessage === "string" ? item.errorMessage.trim() : "",
    }))
    .filter((item) => item.message);
}

function isLandscapeKeyframeVersion(version: StageVersion) {
  const outputSummary = version.outputSummary ?? {};
  const width = summaryNumberValue(outputSummary, "width") || Number(version.asset?.width ?? 0);
  const height = summaryNumberValue(outputSummary, "height") || Number(version.asset?.height ?? 0);
  if (Number.isFinite(width) && Number.isFinite(height) && width > 0 && height > 0) {
    return width > height;
  }
  const aspectRatio = selectedWorkflow.value?.aspectRatio || "";
  return aspectRatio.trim().startsWith("16:");
}

function keyframePreviewFrames(version: StageVersion, slot?: WorkflowClipSlot): PreviewFrame[] {
  const outputSummary = version.outputSummary ?? {};
  const firstFrameUrl = summaryUrlValue(outputSummary, "startFrameUrl", "firstFrameUrl");
  const frameFailures = summaryFrameFailures(outputSummary);
  const lastFailure = frameFailures.find((item) => item.role === "last");
  const lastFrameUrl = summaryUrlValue(outputSummary, "endFrameUrl", "lastFrameUrl")
    || (!lastFailure ? summaryUrlValue(outputSummary, "fileUrl") || (typeof version.previewUrl === "string" ? version.previewUrl : "") : "");
  const hasExplicitFirstSelection = slot ? slot.keyframeVersions.some((item) => Boolean(item.outputSummary?.selectedFirstFrame)) : false;
  const hasExplicitLastSelection = slot ? slot.keyframeVersions.some((item) => Boolean(item.outputSummary?.selectedLastFrame)) : false;
  const frames: PreviewFrame[] = [];
  if (firstFrameUrl) {
    frames.push({
      role: "first",
      label: "首帧",
      url: firstFrameUrl,
      selected: Boolean(outputSummary.selectedFirstFrame || (!hasExplicitFirstSelection && version.selected)),
      regenerable: version.clipIndex <= 1,
    });
  }
  const firstFailure = frameFailures.find((item) => item.role === "first");
  if (!firstFrameUrl && firstFailure) {
    frames.push({
      role: "first",
      label: "首帧",
      url: "",
      selected: false,
      regenerable: version.clipIndex <= 1,
      errorMessage: firstFailure.message,
    });
  }
  if (lastFrameUrl && (!firstFrameUrl || lastFrameUrl !== firstFrameUrl || version.clipIndex === 1)) {
    frames.push({
      role: "last",
      label: "尾帧",
      url: lastFrameUrl,
      selected: Boolean(outputSummary.selectedLastFrame || (!hasExplicitLastSelection && version.selected)),
      regenerable: true,
    });
  }
  if (!lastFrameUrl && lastFailure) {
    frames.push({
      role: "last",
      label: "尾帧",
      url: "",
      selected: false,
      regenerable: true,
      errorMessage: lastFailure.message,
    });
  }
  return frames;
}

function characterSheetKey(sheet: WorkflowCharacterSheet) {
  return sheet.id || `${characterSheetTitle(sheet)}-${characterSheetClipIndex(sheet) ?? "na"}`;
}

function characterSheetClipIndex(sheet: WorkflowCharacterSheet) {
  const candidates = [sheet.syntheticClipIndex, sheet.clipIndex];
  for (const candidate of candidates) {
    const numericValue = Number(candidate);
    if (Number.isInteger(numericValue) && numericValue > 0) {
      return numericValue;
    }
  }
  return null;
}

function characterSheetTitle(sheet: WorkflowCharacterSheet) {
  return sheet.characterName?.trim()
    || sheet.displayName?.trim()
    || sheet.name?.trim()
    || `角色 #${characterSheetClipIndex(sheet) ?? "-"}`;
}

function characterSheetAppearanceSummary(sheet: WorkflowCharacterSheet) {
  return sheet.appearanceSummary?.trim()
    || sheet.appearance?.trim()
    || "暂无角色外观摘要";
}

function characterSheetVersions(sheet: WorkflowCharacterSheet) {
  return sheet.versions?.length ? sheet.versions : (sheet.keyframeVersions ?? []);
}

function characterSheetPreviewFrames(version: StageVersion): PreviewFrame[] {
  const outputSummary = version.outputSummary ?? {};
  const frames: PreviewFrame[] = [];
  const namedFrames = [
    { role: "front", label: "正面", url: summaryUrlValue(outputSummary, "frontViewUrl", "frontImageUrl", "frontUrl") },
    { role: "side", label: "侧面", url: summaryUrlValue(outputSummary, "sideViewUrl", "sideImageUrl", "sideUrl", "profileViewUrl") },
    { role: "back", label: "背面", url: summaryUrlValue(outputSummary, "backViewUrl", "backImageUrl", "backUrl") },
  ].filter((frame) => frame.url);
  if (namedFrames.length) {
    return namedFrames;
  }
  const listFrames = summaryUrlListValue(outputSummary, "threeViewUrls", "viewUrls", "sheetUrls", "images");
  if (listFrames.length) {
    const labels = ["正面", "侧面", "背面"];
    return listFrames.map((url, index) => ({
      role: `view-${index + 1}`,
      label: labels[index] || `视图 ${index + 1}`,
      url,
    }));
  }
  const previewUrl = summaryUrlValue(outputSummary, "sheetUrl", "previewUrl", "fileUrl")
    || (typeof version.previewUrl === "string" ? version.previewUrl : "");
  if (previewUrl) {
    frames.push({
      role: "sheet",
      label: "三视图",
      url: previewUrl,
    });
  }
  return frames;
}

function selectedCharacterSheetVersion(sheet: WorkflowCharacterSheet) {
  return characterSheetVersions(sheet).find((version) => version.selected) ?? null;
}

function isCharacterAssetPickerOpen(sheet: WorkflowCharacterSheet) {
  return characterAssetPicker.openKey === characterSheetKey(sheet);
}

async function openCharacterAssetPicker(sheet: WorkflowCharacterSheet) {
  characterAssetPicker.openKey = characterSheetKey(sheet);
  characterAssetPicker.keyword = characterSheetTitle(sheet);
  characterAssetPicker.model = "";
  await loadCharacterAssetCandidates(sheet);
}

function closeCharacterAssetPicker() {
  characterAssetPicker.openKey = "";
  characterAssetPicker.error = "";
  characterAssetPicker.assets = [];
}

function materialAssetPreviewUrl(asset: MaterialAssetLibraryItem) {
  return asset.previewUrl || asset.fileUrl || asset.remoteUrl || "";
}

function materialAssetModelLabel(asset: MaterialAssetLibraryItem) {
  return asset.originModel || asset.originProvider || "未记录模型";
}

async function loadCharacterAssetCandidates(sheet: WorkflowCharacterSheet) {
  const expectedKey = characterSheetKey(sheet);
  characterAssetPicker.openKey = expectedKey;
  characterAssetPicker.loading = true;
  characterAssetPicker.error = "";
  try {
    const assets = await fetchMaterialAssets({
      q: characterAssetPicker.keyword.trim() || characterSheetTitle(sheet),
      assetType: "character_sheet",
      model: characterAssetPicker.model.trim() || undefined,
    });
    if (characterAssetPicker.openKey !== expectedKey) {
      return;
    }
    characterAssetPicker.assets = assets.filter((asset) => asset.mediaType === "image" || Boolean(materialAssetPreviewUrl(asset)));
  } catch (error) {
    characterAssetPicker.error = error instanceof Error ? error.message : "角色三视图素材加载失败";
    characterAssetPicker.assets = [];
  } finally {
    if (characterAssetPicker.openKey === expectedKey) {
      characterAssetPicker.loading = false;
    }
  }
}

function applyImagePreviewItem(item: ImagePreviewItem, index: number) {
  imagePreviewState.url = item.url;
  imagePreviewState.alt = item.alt;
  imagePreviewState.caption = item.caption;
  imagePreviewState.currentIndex = index;
}

function openImagePreview(url: string, alt: string) {
  if (!url) {
    return;
  }
  const item = { url, alt, caption: alt };
  imagePreviewState.open = true;
  imagePreviewState.gallery = [item];
  applyImagePreviewItem(item, 0);
}

function openKeyframeImagePreview(version: StageVersion, frame: PreviewFrame) {
  if (!frame.url) {
    return;
  }
  const frames = keyframePreviewFrames(version).filter((item) => item.url);
  const gallery = frames.map((item) => ({
    url: item.url,
    alt: `${version.title}${item.label}`,
    caption: `${version.title} ${item.label}`,
  }));
  const currentIndex = Math.max(0, frames.findIndex((item) => item.role === frame.role));
  const currentItem = gallery[currentIndex];
  if (!currentItem) {
    openImagePreview(frame.url, `${version.title}${frame.label}`);
    return;
  }
  imagePreviewState.open = true;
  imagePreviewState.gallery = gallery;
  applyImagePreviewItem(currentItem, currentIndex);
}

function closeImagePreview() {
  imagePreviewState.open = false;
  imagePreviewState.url = "";
  imagePreviewState.alt = "";
  imagePreviewState.caption = "";
  imagePreviewState.gallery = [];
  imagePreviewState.currentIndex = 0;
}

function switchImagePreviewFrame(direction: 1 | -1) {
  if (!imagePreviewState.open || imagePreviewState.gallery.length < 2) {
    return;
  }
  const nextIndex = (imagePreviewState.currentIndex + direction + imagePreviewState.gallery.length) % imagePreviewState.gallery.length;
  applyImagePreviewItem(imagePreviewState.gallery[nextIndex], nextIndex);
}

function handleImagePreviewKeydown(event: KeyboardEvent) {
  if (!imagePreviewState.open) {
    return;
  }
  if (event.key === "Escape") {
    event.preventDefault();
    closeImagePreview();
    return;
  }
  if (event.key === "ArrowLeft") {
    event.preventDefault();
    switchImagePreviewFrame(-1);
    return;
  }
  if (event.key === "ArrowRight") {
    event.preventDefault();
    switchImagePreviewFrame(1);
  }
}

function selectedKeyframeVersion(slot: WorkflowClipSlot) {
  return slot.keyframeVersions.find((version) => version.selected) ?? null;
}

function selectedKeyframeFrameVersion(slot: WorkflowClipSlot, frameRole: string) {
  const selectionKey = frameRole === "first" ? "selectedFirstFrame" : "selectedLastFrame";
  return slot.keyframeVersions.find((version) => Boolean(version.outputSummary?.[selectionKey])) ?? selectedKeyframeVersion(slot);
}

function selectedKeyframePreviewFrames(slot: WorkflowClipSlot) {
  const frames: PreviewFrame[] = [];
  const firstVersion = selectedKeyframeFrameVersion(slot, "first");
  const firstFrame = firstVersion ? keyframePreviewFrames(firstVersion, slot).find((frame) => frame.role === "first") : null;
  if (firstFrame) {
    frames.push(firstFrame);
  }
  const lastVersion = selectedKeyframeFrameVersion(slot, "last");
  const lastFrame = lastVersion ? keyframePreviewFrames(lastVersion, slot).find((frame) => frame.role === "last") : null;
  if (lastFrame && (!firstFrame || lastFrame.url !== firstFrame.url || slot.clipIndex === 1)) {
    frames.push(lastFrame);
  }
  return frames;
}

const normalizedStoryboardManualDurationSeconds = computed(() => parseStoryboardDurationSeconds(storyboardManualDurationSeconds.value));
const storyboardManualDurationHint = computed(() =>
  `手动模式下镜头时长限制为 ${STORYBOARD_MANUAL_DURATION_MIN_SECONDS}-${STORYBOARD_MANUAL_DURATION_MAX_SECONDS} 秒。`
);
const storyboardManualDurationValidationMessage = computed(() => {
  if (storyboardDurationMode.value === "auto") {
    return "";
  }
  if (!storyboardManualDurationSeconds.value.trim()) {
    return `请先填写合法的镜头时长（${STORYBOARD_MANUAL_DURATION_MIN_SECONDS}-${STORYBOARD_MANUAL_DURATION_MAX_SECONDS} 秒）`;
  }
  if (normalizedStoryboardManualDurationSeconds.value === null) {
    return `请先填写合法的镜头时长（${STORYBOARD_MANUAL_DURATION_MIN_SECONDS}-${STORYBOARD_MANUAL_DURATION_MAX_SECONDS} 秒）`;
  }
  if (
    normalizedStoryboardManualDurationSeconds.value < STORYBOARD_MANUAL_DURATION_MIN_SECONDS
    || normalizedStoryboardManualDurationSeconds.value > STORYBOARD_MANUAL_DURATION_MAX_SECONDS
  ) {
    return `手动模式的镜头时长需在 ${STORYBOARD_MANUAL_DURATION_MIN_SECONDS}-${STORYBOARD_MANUAL_DURATION_MAX_SECONDS} 秒之间`;
  }
  return "";
});
const isStoryboardDurationConfigured = computed(() => {
  if (storyboardDurationMode.value === "auto") {
    return true;
  }
  return Boolean(normalizedStoryboardManualDurationSeconds.value !== null && !storyboardManualDurationValidationMessage.value);
});

function setStageFieldIndex(index: number) {
  const maxIndex = Math.max(activeStageSteps.value.length - 1, 0);
  stageFieldIndexMap[activeCreateStage.value] = Math.min(Math.max(index, 0), maxIndex);
}

function nextStageField() {
  if (!activeStageSteps.value.length) {
    return;
  }
  if (activeStageFieldIndex.value >= activeStageSteps.value.length - 1) {
    setStageFieldIndex(0);
    return;
  }
  setStageFieldIndex(activeStageFieldIndex.value + 1);
}

function previousStageField() {
  if (!activeStageSteps.value.length) {
    return;
  }
  setStageFieldIndex(activeStageFieldIndex.value - 1);
}

function openCreateReview() {
  createError.value = "";
  createReviewFlipped.value = true;
}

async function closeCreateReview() {
  createError.value = "";
  createReviewFlipped.value = false;
  createComposerVisible.value = false;
  selectedWorkflow.value = null;
  if (selectedWorkflowId.value) {
    await router.push("/workflows");
  }
}

function startCreateWorkflow() {
  createError.value = "";
  createComposerVisible.value = true;
  createReviewFlipped.value = true;
}

function syncWorkflowSettingsDraft(workflow: WorkflowDetail) {
  const minDurationSeconds = workflow.minDurationSeconds === null || workflow.minDurationSeconds === undefined
    ? STORYBOARD_MANUAL_DURATION_MIN_SECONDS
    : workflow.minDurationSeconds;
  const maxDurationSeconds = workflow.maxDurationSeconds === null || workflow.maxDurationSeconds === undefined
    ? STORYBOARD_MANUAL_DURATION_MAX_SECONDS
    : workflow.maxDurationSeconds;
  const durationMode = workflow.durationMode === "manual" ? "manual" : "auto";
  workflowSettingsDraft.aspectRatio = workflow.aspectRatio || "9:16";
  workflowSettingsDraft.stylePreset = workflow.stylePreset || "";
  workflowSettingsDraft.textAnalysisModel = workflow.textAnalysisModel || "";
  workflowSettingsDraft.imageModel = workflow.imageModel || "";
  workflowSettingsDraft.videoModel = workflow.videoModel || "";
  workflowSettingsDraft.videoSize = workflow.videoSize || "";
  workflowSettingsDraft.keyframeSeed = workflow.keyframeSeed === null || workflow.keyframeSeed === undefined ? "" : String(workflow.keyframeSeed);
  workflowSettingsDraft.videoSeed = workflow.videoSeed === null || workflow.videoSeed === undefined ? "" : String(workflow.videoSeed);
  workflowSettingsDraft.durationMode = durationMode;
  workflowSettingsDraft.minDurationSeconds = String(minDurationSeconds);
  workflowSettingsDraft.maxDurationSeconds = String(maxDurationSeconds);
  syncVideoSizeSelection(workflowSettingsDraft, workflow.videoSize);
}

function applyWorkflowDrafts(workflow: WorkflowDetail | null) {
  if (!workflow) {
    workflowRatingDraft.value = "5";
    workflowRatingNoteDraft.value = "";
    workflowSettingsOpen.value = false;
    return;
  }
  syncWorkflowSettingsDraft(workflow);
  workflowRatingDraft.value = String(workflow.effectRating ?? 5);
  workflowRatingNoteDraft.value = workflow.effectRatingNote ?? "";
  for (const version of workflow.storyboardVersions) {
    stageRatingDrafts[version.id] = String(version.rating ?? 5);
    stageNoteDrafts[version.id] = version.ratingNote ?? "";
    storyboardAdjustmentDrafts[version.id] ??= "";
  }
  for (const sheet of workflow.characterSheets ?? []) {
    for (const version of characterSheetVersions(sheet)) {
      stageRatingDrafts[version.id] = String(version.rating ?? 5);
      stageNoteDrafts[version.id] = version.ratingNote ?? "";
    }
  }
  for (const slot of workflow.clipSlots) {
    for (const version of [...slot.keyframeVersions, ...slot.videoVersions]) {
      stageRatingDrafts[version.id] = String(version.rating ?? 5);
      stageNoteDrafts[version.id] = version.ratingNote ?? "";
    }
  }
}

function openWorkflow(workflowId: string, preferredStage?: string | null) {
  createComposerVisible.value = false;
  createReviewFlipped.value = false;
  const nextStage = normalizeDetailStage(preferredStage) ?? normalizeDetailStage(route.query.stage) ?? activeCreateStage.value;
  void router.push({
    path: `/workflows/${workflowId}`,
    query: { stage: nextStage },
  });
}

function switchWorkflowStage(stage: CreateStageKey) {
  activeCreateStage.value = stage;
  if (!selectedWorkflowId.value) {
    return;
  }
  const currentRouteStage = normalizeDetailStage(route.query.stage);
  if (currentRouteStage === stage) {
    return;
  }
  void router.replace({
    path: route.path,
    query: {
      ...route.query,
      stage,
    },
  });
}

function setStageRatingDraft(versionId: string, score: number) {
  stageRatingDrafts[versionId] = String(score);
}

async function loadOptions() {
  loadingOptions.value = true;
  try {
    const result = await fetchGenerationOptions();
    options.value = result;
    if (!createForm.stylePreset) {
      createForm.stylePreset = result.defaultStylePreset || result.stylePresets[0]?.key || "cinematic";
    }
    if (!createForm.textAnalysisModel) {
      createForm.textAnalysisModel = result.defaultTextAnalysisModel || result.textAnalysisModels?.[0]?.value || "";
    }
    if (!createForm.imageModel) {
      createForm.imageModel = result.imageModels?.[0]?.value || "";
    }
    if (!createForm.videoModel) {
      createForm.videoModel = result.defaultVideoModel || result.videoModels[0]?.value || "";
    }
    if (!createForm.videoSize) {
      syncVideoSizeSelection(createForm, result.defaultVideoSize);
    }
    if (!createForm.aspectRatio) {
      createForm.aspectRatio = (result.defaultAspectRatio as "9:16" | "16:9" | null) || "9:16";
    }
  } finally {
    loadingOptions.value = false;
  }
}

async function loadWorkflows() {
  loadingWorkflows.value = true;
  listError.value = "";
  try {
    workflows.value = await fetchWorkflows();
  } catch (error) {
    listError.value = error instanceof Error ? error.message : "工作流列表加载失败";
  } finally {
    loadingWorkflows.value = false;
  }
}

async function loadWorkflowDetail(workflowId: string) {
  loadingDetail.value = true;
  detailError.value = "";
  try {
    selectedWorkflow.value = await fetchWorkflow(workflowId);
    const routeStage = normalizeDetailStage(route.query.stage);
    const resolvedStage = routeStage ?? normalizeDetailStage(selectedWorkflow.value?.currentStage) ?? "storyboard";
    activeCreateStage.value = resolvedStage;
    if (routeStage !== resolvedStage) {
      await router.replace({
        path: route.path,
        query: {
          ...route.query,
          stage: resolvedStage,
        },
      });
    }
    applyWorkflowDrafts(selectedWorkflow.value);
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : "工作流详情加载失败";
    selectedWorkflow.value = null;
  } finally {
    loadingDetail.value = false;
  }
}

async function reloadCurrentWorkflow() {
  if (selectedWorkflowId.value) {
    await loadWorkflowDetail(selectedWorkflowId.value);
    await loadWorkflows();
  }
}

function buildCreatePayload(): CreateWorkflowRequest {
  const fixedDurationSeconds = storyboardDurationMode.value === "manual" ? normalizedStoryboardManualDurationSeconds.value : null;
  return {
    title: createForm.title.trim(),
    transcriptText: createForm.transcriptText.trim() || null,
    globalPrompt: createForm.globalPrompt.trim() || null,
    aspectRatio: createForm.aspectRatio as "9:16" | "16:9",
    stylePreset: createForm.stylePreset || null,
    textAnalysisModel: createForm.textAnalysisModel,
    imageModel: createForm.imageModel,
    videoModel: createForm.videoModel,
    videoSize: createForm.videoSize || null,
    keyframeSeed: createForm.keyframeSeed === "" ? null : Number(createForm.keyframeSeed),
    videoSeed: createForm.videoSeed === "" ? null : Number(createForm.videoSeed),
    durationMode: storyboardDurationMode.value,
    minDurationSeconds: fixedDurationSeconds,
    maxDurationSeconds: fixedDurationSeconds,
  };
}

function buildWorkflowSettingsPayload(): UpdateWorkflowSettingsRequest {
  return {
    aspectRatio: workflowSettingsDraft.aspectRatio,
    stylePreset: workflowSettingsDraft.stylePreset,
    textAnalysisModel: workflowSettingsDraft.textAnalysisModel,
    imageModel: workflowSettingsDraft.imageModel,
    videoModel: workflowSettingsDraft.videoModel,
    videoSize: workflowSettingsDraft.videoSize,
    keyframeSeed: optionalInteger(workflowSettingsDraft.keyframeSeed),
    videoSeed: optionalInteger(workflowSettingsDraft.videoSeed),
    durationMode: workflowSettingsDraft.durationMode,
    minDurationSeconds: workflowSettingsDraft.durationMode === "auto" ? null : optionalInteger(workflowSettingsDraft.minDurationSeconds),
    maxDurationSeconds: workflowSettingsDraft.durationMode === "auto" ? null : optionalInteger(workflowSettingsDraft.maxDurationSeconds),
  };
}

async function handleCreateWorkflow() {
  createError.value = "";
  if (storyboardDurationMode.value === "manual" && storyboardManualDurationValidationMessage.value) {
    createError.value = storyboardManualDurationValidationMessage.value;
    createReviewFlipped.value = false;
    return;
  }
  creatingWorkflow.value = true;
  try {
    const workflow = await createWorkflow(buildCreatePayload());
    createForm.title = "";
    createForm.transcriptText = "";
    createForm.globalPrompt = "";
    createForm.keyframeSeed = "";
    createForm.videoSeed = "";
    storyboardDurationMode.value = "auto";
    storyboardManualDurationSeconds.value = "8";
    createComposerVisible.value = false;
    createReviewFlipped.value = false;
    await loadWorkflows();
    openWorkflow(workflow.id, workflow.currentStage);
  } catch (error) {
    createError.value = error instanceof Error ? error.message : "创建工作流失败";
    createReviewFlipped.value = false;
  } finally {
    creatingWorkflow.value = false;
  }
}

function resetTransientStageState() {
  closeCharacterAssetPicker();
  workflowSettingsOpen.value = false;
}

async function runAndRefresh(actionKey: string, runner: () => Promise<WorkflowDetail>) {
  busyActionKey.value = actionKey;
  detailError.value = "";
  try {
    selectedWorkflow.value = await runner();
    applyWorkflowDrafts(selectedWorkflow.value);
    await loadWorkflows();
    return true;
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : "操作失败";
    return false;
  } finally {
    busyActionKey.value = "";
  }
}

async function handleUpdateWorkflowSettings() {
  if (!selectedWorkflowId.value || workflowSettingsValidationMessage.value) {
    return;
  }
  const succeeded = await runAndRefresh("workflow-settings", () => updateWorkflowSettings(selectedWorkflowId.value, buildWorkflowSettingsPayload()));
  if (succeeded) {
    workflowSettingsOpen.value = false;
  }
}

async function handleGenerateStoryboard() {
  if (!selectedWorkflowId.value) {
    return;
  }
  await runAndRefresh("storyboard", () => generateStoryboard(selectedWorkflowId.value));
}

async function handleAdjustStoryboard(versionId: string) {
  if (!selectedWorkflowId.value) {
    return;
  }
  const prompt = (storyboardAdjustmentDrafts[versionId] || "").trim();
  const succeeded = await runAndRefresh(`storyboard-adjust-${versionId}`, () => adjustStoryboard(selectedWorkflowId.value, versionId, prompt));
  if (succeeded) {
    storyboardAdjustmentDrafts[versionId] = "";
  }
}

async function handleSelectStoryboard(versionId: string) {
  if (!selectedWorkflowId.value) {
    return;
  }
  await runAndRefresh(versionId, () => selectStoryboard(selectedWorkflowId.value, versionId));
}

async function handleGenerateKeyframe(clipIndex: number) {
  if (!selectedWorkflowId.value) {
    return;
  }
  await runAndRefresh(`keyframe-${clipIndex}`, () => generateKeyframe(selectedWorkflowId.value, clipIndex));
}

async function handleGenerateKeyframeFrame(clipIndex: number, frameRole: string) {
  if (!selectedWorkflowId.value) {
    return;
  }
  await runAndRefresh(`keyframe-${clipIndex}-${frameRole}`, () => generateKeyframeFrame(selectedWorkflowId.value, clipIndex, frameRole));
}

async function handleSelectKeyframe(clipIndex: number, versionId: string) {
  if (!selectedWorkflowId.value) {
    return;
  }
  await runAndRefresh(versionId, () => selectKeyframe(selectedWorkflowId.value, clipIndex, versionId));
}

async function handleSelectKeyframeFrame(clipIndex: number, versionId: string, frameRole: string) {
  if (!selectedWorkflowId.value) {
    return;
  }
  await runAndRefresh(`${versionId}-${frameRole}`, () => selectKeyframeFrame(selectedWorkflowId.value, clipIndex, versionId, frameRole));
}

async function handleSelectCharacterSheetAsset(sheet: WorkflowCharacterSheet, assetId: string) {
  const clipIndex = characterSheetClipIndex(sheet);
  if (!selectedWorkflowId.value || clipIndex === null) {
    return;
  }
  busyActionKey.value = `character-sheet-asset-${clipIndex}`;
  detailError.value = "";
  try {
    await selectCharacterSheetAsset(selectedWorkflowId.value, clipIndex, assetId);
    closeCharacterAssetPicker();
    await reloadCurrentWorkflow();
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : "角色三视图素材选择失败";
  } finally {
    busyActionKey.value = "";
  }
}

async function handleGenerateVideo(clipIndex: number) {
  if (!selectedWorkflowId.value) {
    return;
  }
  await runAndRefresh(`video-${clipIndex}`, () => generateVideo(selectedWorkflowId.value, clipIndex));
}

async function handleSelectVideo(clipIndex: number, versionId: string) {
  if (!selectedWorkflowId.value) {
    return;
  }
  await runAndRefresh(versionId, () => selectVideo(selectedWorkflowId.value, clipIndex, versionId));
}

async function handleFinalize() {
  if (!selectedWorkflowId.value) {
    return;
  }
  await runAndRefresh("finalize", () => finalizeWorkflow(selectedWorkflowId.value));
}

async function handleRateWorkflow() {
  if (!selectedWorkflowId.value) {
    return;
  }
  await runAndRefresh("workflow-rating", () =>
    rateWorkflow(selectedWorkflowId.value, {
      effectRating: Number(workflowRatingDraft.value || 5),
      effectRatingNote: workflowRatingNoteDraft.value.trim() || null,
    })
  );
}

async function handleRateStageVersion(version: StageVersion) {
  if (!selectedWorkflowId.value) {
    return;
  }
  const actionKey = `rate-${version.id}`;
  await runAndRefresh(actionKey, () =>
    rateStageVersion(selectedWorkflowId.value, version.id, {
      effectRating: Number(stageRatingDrafts[version.id] || version.rating || 5),
      effectRatingNote: stageNoteDrafts[version.id]?.trim() || null,
    })
  );
}

function stageTypeLabel(stageType: StageVersion["stageType"]) {
  switch (stageType) {
    case "storyboard":
      return "分镜";
    case "keyframe":
      return "关键帧";
    case "video":
      return "视频";
    default:
      return "版本";
  }
}

function deleteWorkflowConfirmMessage(workflow: WorkflowSummary) {
  return `删除后不可恢复，工作流《${workflow.title}》及其所有生成版本都会一并删除。确认继续吗？`;
}

function deleteVersionConfirmMessage(version: StageVersion) {
  const stageLabel = stageTypeLabel(version.stageType);
  if (version.stageType === "storyboard") {
    return `删除后不可恢复。删除该${stageLabel}版本时，与它关联的关键帧和视频版本也会一并删除。确认继续吗？`;
  }
  if (version.stageType === "keyframe") {
    return `删除后不可恢复。删除该${stageLabel}版本时，依赖它生成的视频版本也会一并删除。确认继续吗？`;
  }
  return `删除后不可恢复，确认删除这个${stageLabel}版本吗？`;
}

async function handleDeleteWorkflow(workflow: WorkflowSummary) {
  if (!window.confirm(deleteWorkflowConfirmMessage(workflow))) {
    return;
  }
  const actionKey = `delete-workflow-${workflow.id}`;
  busyActionKey.value = actionKey;
  listError.value = "";
  detailError.value = "";
  try {
    const result: WorkflowDeleteResult = await deleteWorkflow(workflow.id);
    if (result.deleted && selectedWorkflowId.value === workflow.id) {
      selectedWorkflow.value = null;
      await router.push("/workflows");
    }
    await loadWorkflows();
  } catch (error) {
    const message = error instanceof Error ? error.message : "工作流删除失败";
    listError.value = message;
    detailError.value = message;
  } finally {
    busyActionKey.value = "";
  }
}

async function handleDeleteStageVersion(version: StageVersion) {
  if (!selectedWorkflowId.value || !window.confirm(deleteVersionConfirmMessage(version))) {
    return;
  }
  const actionKey = `delete-${version.id}`;
  busyActionKey.value = actionKey;
  detailError.value = "";
  try {
    selectedWorkflow.value = await deleteStageVersion(selectedWorkflowId.value, version.id);
    applyWorkflowDrafts(selectedWorkflow.value);
    await loadWorkflows();
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : "版本删除失败";
  } finally {
    busyActionKey.value = "";
  }
}

async function handleReuseAsset(assetId: string, versionId: string) {
  if (!assetId) {
    return;
  }
  busyActionKey.value = `reuse-${versionId}`;
  detailError.value = "";
  try {
    const workflow = await reuseMaterialAsset(assetId, { mode: "clone" });
    await loadWorkflows();
    openWorkflow(workflow.id, workflow.currentStage);
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : "素材复用失败";
  } finally {
    busyActionKey.value = "";
  }
}

watch(
  () => [createForm.videoModel, createForm.aspectRatio, catalogVideoSizeOptions.value] as const,
  () => {
    syncVideoSizeSelection(createForm, createForm.videoSize);
  }
);

watch(
  () => [workflowSettingsDraft.videoModel, workflowSettingsDraft.aspectRatio, catalogVideoSizeOptions.value] as const,
  () => {
    syncVideoSizeSelection(workflowSettingsDraft, workflowSettingsDraft.videoSize);
  }
);

watch(
  () => route.query.stage,
  (stage) => {
    if (!selectedWorkflowId.value) {
      return;
    }
    const resolvedStage = normalizeDetailStage(stage);
    if (resolvedStage && resolvedStage !== activeCreateStage.value) {
      activeCreateStage.value = resolvedStage;
    }
  }
);

watch(
  () => selectedWorkflowId.value,
  (workflowId) => {
    if (!workflowId) {
      selectedWorkflow.value = null;
      resetTransientStageState();
      return;
    }
    resetTransientStageState();
    void loadWorkflowDetail(workflowId);
  },
  { immediate: true }
);

onMounted(async () => {
  window.addEventListener("keydown", handleImagePreviewKeydown);
  await loadOptions();
  await loadWorkflows();
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleImagePreviewKeydown);
});
</script>

<style scoped>
.workflow-view {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 20px;
  height: 100%;
  min-height: 0;
  padding: 22px;
  color: var(--text-strong);
}

.workflow-rail,
.workflow-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 0;
}

.workflow-rail {
  overflow: hidden;
}

.workflow-rail-flip-shell {
  flex: 1 1 auto;
  min-height: 0;
  perspective: 2200px;
}

.workflow-rail-flip {
  display: grid;
  min-height: 100%;
  transform-style: preserve-3d;
  transition: transform 0.72s cubic-bezier(0.22, 0.8, 0.2, 1);
}

.workflow-rail-flip-active {
  transform: rotateY(180deg);
}

.workflow-rail-flip__face {
  grid-area: 1 / 1;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  transform-style: preserve-3d;
}

.workflow-rail-flip__face-front {
  transform: rotateY(0deg);
}

.workflow-rail-flip__face-back {
  transform: rotateY(180deg);
}

.workflow-rail-flip__face-back.workflow-rail-panel {
  padding-top: 18px;
}

.workflow-rail-flip-active .workflow-rail-flip__face-front {
  pointer-events: none;
}

.workflow-rail-flip:not(.workflow-rail-flip-active) .workflow-rail-flip__face-back {
  pointer-events: none;
}

.workflow-panel {
  padding: 22px;
  min-height: 0;
}

.workflow-rail-panel {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: calc(100vh - 132px);
  min-height: 0;
  max-height: calc(100vh - 132px);
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-top: 28px;
}

.workflow-rail__hero {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.workflow-rail__title-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.workflow-rail__title-row h2 {
  margin: 0;
}

.workflow-rail__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.workflow-rail__actions-inline {
  justify-content: flex-start;
}

.workflow-rail__search {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.workflow-rail__search span {
  font-size: 0.84rem;
  color: var(--text-body);
}

.workflow-list-shell {
  flex: 1 0 auto;
  min-height: 0;
  overflow: visible;
  padding-right: 4px;
}

.workflow-create-shell {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 14px 24px 24px;
  border-radius: 28px;
  background: #fff;
  border: 1px solid rgba(15, 20, 25, 0.06);
}

.workflow-main {
  overflow-y: auto;
  padding-right: 4px;
}

.workflow-main > * {
  flex: 0 0 auto;
}

.workflow-create-shell__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0;
}

.workflow-create-canvas {
  display: block;
}

.workflow-review-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 100%;
}

.workflow-review-card__head {
  align-items: flex-start;
}

.workflow-review-card-rail {
  height: 100%;
}

.workflow-review-card__status {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.workflow-review-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.workflow-review-grid-rail {
  grid-template-columns: 1fr;
  overflow-y: auto;
  padding-right: 4px;
}

.workflow-review-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(15, 20, 25, 0.06);
  background: #fff;
}

.workflow-review-section__head h4 {
  margin: 6px 0 0;
}

.workflow-review-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.workflow-review-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border-radius: 16px;
  background: #f8fafb;
  border: 1px solid rgba(15, 20, 25, 0.06);
}

.workflow-review-item strong {
  display: block;
  margin-bottom: 6px;
}

.workflow-review-item p {
  margin: 0;
  color: var(--text-body);
  line-height: 1.5;
}

.workflow-review-item__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.workflow-review-chip-success {
  border-color: rgba(88, 212, 136, 0.38);
  background: rgba(88, 212, 136, 0.12);
  color: #1b9f63;
}

.workflow-review-chip-warning {
  border-color: rgba(255, 180, 92, 0.46);
  background: rgba(255, 180, 92, 0.14);
  color: #9a6100;
}

.workflow-review-chip-muted {
  border-color: rgba(15, 20, 25, 0.08);
  background: #f8fafb;
  color: var(--text-muted);
}

.surface-chip-quiet {
  border-color: rgba(15, 20, 25, 0.08);
  background: #f8fafb;
  color: var(--text-body);
}

.workflow-review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.workflow-review-actions-rail {
  margin-top: auto;
}

.workflow-stage-create {
  display: grid;
  grid-template-columns: 1fr;
  grid-template-areas:
    "base"
    "focus";
  gap: 16px;
  align-items: start;
}

.stage-config-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  border-radius: 22px;
  background: #fff;
  border: 1px solid rgba(15, 20, 25, 0.06);
  box-shadow: var(--shadow-soft);
}

.stage-config-card--base {
  grid-area: base;
}

.stage-config-card--focus {
  grid-area: focus;
}

.stage-config-card__head {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stage-config-card__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.stage-config-card__head h3 {
  margin: 0;
  font-size: 1.08rem;
}

.stage-progress-copy {
  margin: 8px 0 0;
  max-width: 620px;
  color: var(--text-body);
  line-height: 1.6;
}

.stage-config-card__head :deep(.surface-chip) {
  width: fit-content;
}

.stage-switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.stage-switch-btn {
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid rgba(15, 20, 25, 0.08);
  background: #fff;
  color: var(--text-body);
  font-size: 0.88rem;
  font-weight: 700;
  transition: border-color 0.2s ease, background 0.2s ease, transform 0.2s ease, color 0.2s ease;
}

.stage-switch-btn:hover,
.stage-switch-btn-active {
  border-color: rgba(255, 180, 92, 0.54);
  background: rgba(255, 180, 92, 0.12);
  color: #9a6100;
  transform: translateY(-1px);
}

.workflow-view :deep(.surface-panel),
.workflow-view :deep(.surface-tile),
.workflow-view :deep(.surface-chip),
.workflow-view :deep(.status-pill),
.workflow-view :deep(.action-chip) {
  border-color: rgba(15, 20, 25, 0.08);
  background: #fff;
  color: var(--text-body);
  box-shadow: var(--shadow-soft);
}

.workflow-view :deep(.field-input),
.workflow-view :deep(.field-textarea),
.workflow-view :deep(.field-select) {
  border-color: rgba(15, 20, 25, 0.08);
  background: #fff;
  color: var(--text-strong);
}

.workflow-view :deep(.field-input::placeholder),
.workflow-view :deep(.field-textarea::placeholder) {
  color: #9aa5ad;
}

.workflow-view :deep(.btn-secondary),
.workflow-view :deep(.btn-ghost),
.workflow-view :deep(.shell-icon-btn) {
  border-color: rgba(15, 20, 25, 0.08);
  background: #fff;
  color: var(--text-strong);
  box-shadow: none;
}

.workflow-view :deep(.btn-primary) {
  background: var(--bg-accent);
  color: #fff;
}

.stage-config-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.stage-config-fields--single {
  grid-template-columns: minmax(280px, 0.92fr) minmax(0, 1.08fr);
  grid-template-areas:
    "title title"
    "prompt body";
  align-items: start;
}

.stage-config-field-title {
  grid-area: title;
}

.stage-config-field-body {
  grid-area: body;
}

.stage-config-field-prompt {
  grid-area: prompt;
}

.stage-config-card--focus {
  min-width: 0;
}

.stage-progress-form {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(220px, 0.7fr);
  gap: 16px;
  align-items: start;
}

.stage-progress-form__rail,
.stage-progress-form__panel,
.stage-progress-form__summary {
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
}

.stage-progress-form__rail {
  grid-column: 1 / -1;
  padding: 16px;
}

.stage-progress-form__bar {
  position: relative;
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.stage-progress-form__fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(255, 180, 92, 0.92), rgba(89, 208, 255, 0.82));
}

.stage-progress-form__steps {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.stage-progress-form__step {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 10px 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.78);
  text-align: left;
  transition: border-color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.stage-progress-form__step span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 0.82rem;
  font-weight: 700;
}

.stage-progress-form__step-active,
.stage-progress-form__step:hover {
  border-color: rgba(255, 180, 92, 0.56);
  background: rgba(255, 180, 92, 0.12);
  transform: translateY(-1px);
}

.stage-progress-form__step-done span,
.stage-progress-form__step-active span {
  background: rgba(255, 180, 92, 0.2);
  color: #fff0d2;
}

.stage-progress-form__panel {
  padding: 18px;
}

.stage-progress-form__panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.stage-progress-form__panel-head h4 {
  margin: 6px 0 8px;
  font-size: 1.06rem;
}

.stage-progress-form__panel-head p:last-child {
  margin: 0;
  color: rgba(255, 255, 255, 0.64);
  line-height: 1.6;
}

.stage-progress-form__field {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stage-progress-form__field > span {
  color: rgba(255, 255, 255, 0.74);
  font-size: 0.9rem;
}

.stage-progress-form__field-note,
.stage-progress-form__field-error {
  margin: 0;
  font-size: 0.86rem;
  line-height: 1.5;
}

.stage-progress-form__field-note {
  color: rgba(255, 255, 255, 0.58);
}

.stage-progress-form__field-error {
  color: #ffb4b4;
}

.stage-progress-form__field-slider {
  max-width: 520px;
}

.stage-option-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.stage-option-grid--compact {
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.stage-option-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 84px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.82);
  text-align: left;
  transition: border-color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.stage-option-card span {
  color: rgba(255, 255, 255, 0.56);
  font-size: 0.82rem;
  line-height: 1.5;
}

.stage-option-card:hover,
.stage-option-card-active {
  border-color: rgba(255, 180, 92, 0.56);
  background: rgba(255, 180, 92, 0.12);
  transform: translateY(-1px);
}

.stage-slider__range {
  width: 100%;
  accent-color: rgba(255, 180, 92, 0.96);
}

.stage-slider__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stage-slider__number {
  width: 120px;
}

.stage-toggle-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.stage-toggle-chip {
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.76);
  transition: border-color 0.2s ease, background 0.2s ease;
}

.stage-toggle-chip-active,
.stage-toggle-chip:hover {
  border-color: rgba(255, 180, 92, 0.56);
  background: rgba(255, 180, 92, 0.12);
  color: #fff0d2;
}

.stage-progress-form__actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
}

.stage-progress-form__summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
}

.stage-summary-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.8);
  text-align: left;
  transition: border-color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.stage-summary-card span {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.54);
}

.stage-summary-card-active,
.stage-summary-card:hover {
  border-color: rgba(89, 208, 255, 0.44);
  background: rgba(89, 208, 255, 0.1);
  transform: translateY(-1px);
}

.stage-config-field-body .field-textarea,
.stage-config-field-prompt .field-textarea {
  min-height: 220px;
}

.workflow-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.workflow-panel__head h2 {
  margin: 6px 0 0;
  font-size: 1.2rem;
  font-weight: 700;
}

.workflow-panel__head-spread {
  align-items: center;
}

.workflow-stage-panel__actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.workflow-settings-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.workflow-settings-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.workflow-settings-panel__head h2 {
  margin: 6px 0 0;
  font-size: 1.1rem;
}

.workflow-settings-form {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px 18px;
  align-items: end;
}

.workflow-settings-field-model {
  grid-column: span 3;
}

.workflow-settings-field-compact {
  grid-column: span 3;
}

.workflow-settings-field-short,
.workflow-settings-field-mode,
.workflow-settings-field-duration {
  grid-column: span 2;
}

.workflow-settings-form__actions {
  grid-column: span 6;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  min-height: 42px;
  align-self: end;
}

.workflow-settings-form__actions .btn-primary {
  min-width: 132px;
}

.workflow-eyebrow {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.56);
}

.workflow-form__grid,
.workflow-list,
.workflow-summary-grid,
.clip-stack,
.clip-version-list,
.version-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.workflow-form {
  gap: 14px;
}

.workflow-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.workflow-form__footer {
  display: flex;
  justify-content: flex-end;
}

.workflow-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.workflow-field-span-2 {
  grid-column: span 2;
}

.workflow-field span {
  font-size: 0.86rem;
  color: rgba(255, 255, 255, 0.72);
}

.workflow-submit {
  justify-content: center;
}

.workflow-list__item {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  text-align: left;
  transition: border-color 0.2s ease, transform 0.2s ease, background 0.2s ease;
}

.workflow-list__open {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
}

.workflow-list__item:hover,
.workflow-list__item-active {
  border-color: rgba(255, 180, 92, 0.5);
  background: rgba(255, 180, 92, 0.08);
  transform: translateY(-1px);
}

.workflow-list__top,
.workflow-list__meta,
.workflow-list__actions,
.workflow-summary__meta,
.workflow-summary__actions,
.rating-row,
.version-card__actions,
.clip-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.workflow-list__actions {
  justify-content: flex-end;
}

.workflow-list__meta,
.clip-card__desc {
  color: rgba(255, 255, 255, 0.68);
  font-size: 0.88rem;
}

.clip-card__desc {
  display: -webkit-box;
  min-height: 3.1em;
  max-width: 100%;
  overflow: hidden;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.workflow-finalize-box {
  display: grid;
  gap: 10px;
}

.workflow-finalize-box__button {
  width: 100%;
  justify-content: center;
}

.workflow-finalize-box__hint {
  margin: 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 0.84rem;
  line-height: 1.5;
}

.workflow-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
}

.workflow-banner-error,
.workflow-error {
  color: #ffb4b4;
}

.workflow-empty {
  padding: 28px 18px;
  border-radius: 20px;
  border: 1px dashed rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.64);
  text-align: center;
}

.workflow-empty-large {
  padding: 64px 24px;
}

.workflow-empty-large h2 {
  margin: 8px 0 10px;
}

.workflow-empty-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
}

.workflow-empty-prompt p:not(.workflow-eyebrow) {
  max-width: 520px;
  margin: 0;
  color: rgba(255, 255, 255, 0.64);
  line-height: 1.7;
}

.workflow-empty-prompt__actions {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.workflow-empty-nested {
  padding: 18px 14px;
}

.workflow-scroll-hint {
  margin-bottom: 10px;
  font-size: 0.84rem;
  color: rgba(255, 255, 255, 0.54);
}

.workflow-scroll-hint-nested {
  margin-bottom: 12px;
}

.workflow-summary-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
}

.workflow-summary-card h3,
.clip-column__head h4,
.version-card h3 {
  margin: 0;
}

.workflow-stage-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px;
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.028));
}

.workflow-stage-grid {
  display: grid;
  grid-template-columns: minmax(220px, 248px) minmax(0, 1fr);
  gap: 18px;
  min-height: 0;
}

.workflow-stage-content {
  min-width: 0;
  min-height: 0;
}

.workflow-summary-card-compact {
  position: sticky;
  top: 0;
  max-height: 72vh;
  overflow-y: auto;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.035));
}

.workflow-kv {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.workflow-kv__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 0.92rem;
}

.workflow-kv__row span {
  color: rgba(255, 255, 255, 0.6);
}

.workflow-note-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
}

.workflow-note-block span {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.52);
}

.workflow-note-block p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.6;
}

.version-grid {
  display: flex;
  flex-direction: row;
  gap: 16px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 2px 6px 10px 0;
  scroll-snap-type: x proximity;
  overscroll-behavior-x: contain;
  scrollbar-gutter: stable both-edges;
}

.storyboard-version-list {
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: visible;
  padding-right: 0;
  scroll-snap-type: none;
  scrollbar-gutter: auto;
}

.storyboard-version-list .version-card {
  flex: 1 1 auto;
  width: 100%;
}

.storyboard-adjust-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.storyboard-adjust-row__input {
  min-width: 0;
}

.version-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  flex: 0 0 min(560px, calc(100% - 8px));
  min-width: 0;
  scroll-snap-align: start;
}

.version-card-compact {
  padding: 16px;
}

.version-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.version-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.version-card__text {
  margin: 0;
  padding: 14px;
  min-height: 220px;
  max-height: 320px;
  overflow: auto;
  border-radius: 16px;
  background: rgba(4, 6, 10, 0.72);
  color: rgba(255, 255, 255, 0.84);
  font-size: 0.84rem;
  line-height: 1.6;
  word-break: break-word;
}

.version-card__markdown :deep(h1),
.version-card__markdown :deep(h2),
.version-card__markdown :deep(h3),
.version-card__markdown :deep(h4),
.version-card__markdown :deep(h5),
.version-card__markdown :deep(h6) {
  margin: 0 0 14px;
  color: #f8f4e8;
  line-height: 1.4;
}

.version-card__markdown :deep(h1) {
  font-size: 1.28rem;
}

.version-card__markdown :deep(h2) {
  font-size: 1.08rem;
}

.version-card__markdown :deep(p),
.version-card__markdown :deep(ul),
.version-card__markdown :deep(ol),
.version-card__markdown :deep(blockquote),
.version-card__markdown :deep(table),
.version-card__markdown :deep(pre) {
  margin: 0 0 14px;
}

.version-card__markdown :deep(ul),
.version-card__markdown :deep(ol) {
  padding-left: 20px;
}

.version-card__markdown :deep(li + li) {
  margin-top: 6px;
}

.version-card__markdown :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 0.78rem;
}

.version-card__markdown :deep(pre) {
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  overflow: auto;
}

.version-card__markdown :deep(pre code) {
  padding: 0;
  background: transparent;
}

.version-card__markdown :deep(blockquote) {
  padding-left: 12px;
  border-left: 3px solid rgba(255, 180, 92, 0.5);
  color: rgba(255, 255, 255, 0.72);
}

.version-card__markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
}

.version-card__markdown :deep(th),
.version-card__markdown :deep(td) {
  padding: 8px 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
  vertical-align: top;
}

.version-card__markdown :deep(th) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff1cc;
  font-weight: 700;
}

.version-card__markdown :deep(a) {
  color: #ffcf7a;
  text-decoration: none;
}

.version-card__markdown :deep(a:hover) {
  text-decoration: underline;
}

.version-card__markdown :deep(:last-child) {
  margin-bottom: 0;
}

.version-card__textarea {
  min-height: 86px;
}

.keyframe-frame-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.keyframe-frame-grid-landscape {
  grid-template-columns: minmax(0, 1fr);
}

.character-sheet-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.character-sheet-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
}

.character-asset-picker {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(255, 180, 92, 0.22);
  background: rgba(255, 180, 92, 0.06);
}

.character-asset-picker__head,
.character-asset-picker__filters {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.character-asset-picker__head {
  align-items: flex-start;
}

.character-asset-picker__head h4 {
  margin: 4px 0 0;
  font-size: 1rem;
}

.character-asset-picker__filters {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
}

.character-asset-picker__search {
  align-self: end;
  min-height: 40px;
}

.character-asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.character-asset-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(6, 8, 12, 0.36);
}

.character-asset-card__preview {
  width: 100%;
  padding: 0;
  border: 0;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.22);
  cursor: zoom-in;
  overflow: hidden;
}

.character-asset-card__preview img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: contain;
}

.character-asset-card__body {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.character-asset-card__body strong {
  overflow-wrap: anywhere;
  line-height: 1.35;
}

.character-asset-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.character-sheet-frame-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.character-sheet-frame-grid-single {
  grid-template-columns: minmax(0, 1fr);
}

.keyframe-frame-grid-compact {
  max-width: 560px;
}

.keyframe-frame-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
}

.keyframe-frame-card-compact {
  padding: 10px;
}

.keyframe-frame-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.keyframe-frame-card__image {
  aspect-ratio: 9 / 16;
  object-fit: cover;
}

.keyframe-frame-card__image-landscape {
  aspect-ratio: 16 / 9;
  object-fit: contain;
}

.keyframe-frame-card__failure {
  display: grid;
  min-height: 180px;
  place-content: center;
  gap: 6px;
  border: 1px dashed rgba(225, 81, 89, 0.38);
  border-radius: 8px;
  background: rgba(225, 81, 89, 0.08);
  padding: 16px;
  color: #7f1d1d;
  text-align: center;
}

.keyframe-frame-card__failure strong,
.keyframe-frame-card__failure span {
  overflow-wrap: anywhere;
}

.keyframe-frame-card__failure span {
  font-size: 12px;
  line-height: 1.5;
}

.keyframe-frame-card__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.character-sheet-frame-card__image {
  object-fit: contain;
  background: rgba(0, 0, 0, 0.22);
}

.character-sheet-frame-card-single {
  width: 100%;
}

.character-sheet-frame-card__image-single {
  aspect-ratio: auto;
  min-height: 260px;
  max-height: 420px;
}

.character-sheet-preview-trigger {
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: zoom-in;
}

.character-sheet-preview-trigger:focus-visible {
  outline: 2px solid rgba(255, 180, 92, 0.9);
  outline-offset: 4px;
  border-radius: 18px;
}

.image-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: rgba(8, 10, 18, 0.82);
  backdrop-filter: blur(10px);
}

.image-preview-caption {
  position: absolute;
  top: 24px;
  left: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: min(420px, calc(100vw - 180px));
  padding: 10px 18px;
  border: 1px solid rgba(255, 180, 92, 0.44);
  border-radius: 14px;
  background: rgba(18, 22, 30, 0.78);
  color: #ffd38a;
  text-align: center;
  transform: translateX(-50%);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.28);
}

.image-preview-caption strong {
  color: #ff6b6b;
  font-size: 0.98rem;
  font-weight: 800;
}

.image-preview-caption span {
  color: rgba(255, 255, 255, 0.62);
  font-size: 0.78rem;
}

.image-preview-full {
  max-width: min(92vw, 1440px);
  max-height: 82vh;
  border-radius: 20px;
  box-shadow: 0 28px 60px rgba(0, 0, 0, 0.45);
  background: rgba(12, 14, 20, 0.9);
}

.image-preview-close {
  position: absolute;
  top: 24px;
  right: 24px;
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(18, 22, 30, 0.8);
  color: #f7f7f8;
  cursor: pointer;
}

.version-card__image,
.version-card__video,
.final-result-card__video {
  width: 100%;
  border-radius: 16px;
  background: rgba(0, 0, 0, 0.35);
}

.clip-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.03));
}

.clip-card__head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 18px;
}

.clip-card__main {
  min-width: 0;
}

.clip-card__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.clip-card__title-row h3 {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.clip-card__bell {
  flex: 0 0 auto;
}

.clip-card__side-actions {
  display: grid;
  grid-template-columns: 76px 144px;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
}

.clip-card__side-actions-video {
  grid-template-columns: 144px 144px;
}

.clip-card__duration-chip,
.clip-card__selection-chip {
  justify-content: center;
  min-height: 44px;
  padding: 0 10px;
  letter-spacing: 0;
  white-space: nowrap;
}

.clip-card__duration-chip {
  width: 76px;
}

.clip-card__selection-chip {
  width: 144px;
  max-width: 144px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.clip-card__action-button {
  width: 144px;
  min-width: 144px;
  min-height: 44px;
  white-space: nowrap;
  word-break: keep-all;
}

.clip-card__character-chip {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.clip-card__selected-keyframes {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
}

.clip-card__selected-keyframes-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.keyframe-thumb-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.keyframe-thumb-card {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 8px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.025);
}

.keyframe-thumb-card__image {
  width: 52px;
  height: 72px;
  flex: 0 0 auto;
  border-radius: 10px;
  object-fit: cover;
  background: rgba(0, 0, 0, 0.35);
}

.keyframe-thumb-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.clip-stack {
  gap: 18px;
}

.clip-version-list-grid {
  display: flex;
  flex-direction: row;
  gap: 14px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 2px 6px 10px 0;
  scroll-snap-type: x proximity;
  overscroll-behavior-x: contain;
  scrollbar-gutter: stable both-edges;
}

.clip-card__columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.clip-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.clip-column__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.clip-version-list-grid .version-card {
  flex: 0 0 min(360px, calc(100% - 8px));
}

.clip-version-list-grid .version-card-keyframe-landscape {
  flex-basis: min(640px, calc(100% - 8px));
}

.rating-pill {
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
  font-size: 0.82rem;
}

.rating-pill-active {
  border-color: rgba(255, 180, 92, 0.72);
  background: rgba(255, 180, 92, 0.16);
  color: #ffe1b1;
}

.workflow-final-compact {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 6px;
}

.final-result-card {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) 320px;
  gap: 18px;
}

.final-result-card__meta {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: 1280px) {
  .workflow-view {
    grid-template-columns: 1fr;
  }

  .workflow-rail-panel {
    position: static;
    height: auto;
    min-height: auto;
    max-height: none;
    overflow: visible;
    padding-top: 22px;
  }

  .workflow-form__grid,
  .workflow-create-shell__grid,
  .workflow-stage-grid,
  .workflow-review-grid,
  .stage-progress-form,
  .clip-card__columns,
  .final-result-card {
    grid-template-columns: 1fr;
  }

  .workflow-stage-create {
    grid-template-columns: 1fr;
    grid-template-areas:
      "base"
      "focus"
      "submit";
  }

  .workflow-summary-card-compact {
    position: static;
    max-height: none;
  }

  .stage-config-fields--single {
    grid-template-columns: 1fr;
    grid-template-areas:
      "title"
      "body"
      "prompt";
  }

  .stage-config-card__title-row {
    flex-direction: column;
  }

  .stage-progress-form__summary,
  .stage-progress-form__panel,
  .stage-progress-form__rail {
    grid-column: auto;
  }

  .stage-switch-row {
    justify-content: flex-start;
  }

  .workflow-settings-form {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }

  .workflow-settings-form__actions {
    grid-column: span 6;
  }

  .keyframe-frame-grid,
  .character-sheet-frame-grid,
  .keyframe-frame-grid-compact {
    grid-template-columns: 1fr;
    max-width: none;
  }

  .keyframe-thumb-list {
    flex-direction: column;
  }
}

@media (max-width: 720px) {
  .stage-slider__meta,
  .stage-progress-form__actions,
  .workflow-stage-panel__actions,
  .workflow-review-item,
  .workflow-review-actions,
  .workflow-settings-panel__head,
  .workflow-settings-form__actions,
  .workflow-panel__head,
  .clip-card__head {
    flex-direction: column;
    align-items: stretch;
  }

  .workflow-settings-form {
    grid-template-columns: 1fr;
  }

  .workflow-settings-field-model,
  .workflow-settings-field-compact,
  .workflow-settings-field-short,
  .workflow-settings-field-mode,
  .workflow-settings-field-duration,
  .workflow-settings-form__actions {
    grid-column: 1 / -1;
  }

  .workflow-field-span-2 {
    grid-column: span 1;
  }

  .clip-card__head {
    grid-template-columns: 1fr;
  }

  .clip-card__side-actions {
    grid-template-columns: 76px minmax(144px, 1fr);
    justify-content: start;
  }

  .clip-card__side-actions-video {
    grid-template-columns: minmax(144px, 1fr) 144px;
  }

  .clip-card__action-button {
    width: 144px;
    min-width: 144px;
  }
}
</style>
