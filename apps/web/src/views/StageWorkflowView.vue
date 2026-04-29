<template>
  <section class="workflow-canvas-view" :class="{ 'workflow-canvas-view-detail': selectedWorkflowId }">
    <aside class="workflow-project-drawer">
      <div class="workflow-drawer__head">
        <div>
          <p class="workflow-eyebrow">Canvas</p>
          <h1>创作画布</h1>
        </div>
        <button class="drawer-icon-button" type="button" :disabled="loadingWorkflows" title="刷新工作流" @click="loadWorkflows">
          ↻
        </button>
      </div>

      <button
        class="workflow-new-button"
        type="button"
        :disabled="creatingWorkflow || loadingOptions"
        @click="startCreateWorkflow"
      >
        <span>+</span>
        {{ creatingWorkflow ? "创建中..." : "新建画布" }}
      </button>

      <label class="workflow-search-box">
        <span>搜索</span>
        <input v-model="workflowSearch" class="field-input" type="search" placeholder="标题、阶段、状态" />
      </label>

      <p v-if="listError" class="workflow-error">{{ listError }}</p>
      <div v-else-if="loadingWorkflows" class="workflow-empty workflow-empty-soft">正在加载工作流...</div>
      <div v-else-if="!filteredWorkflows.length" class="workflow-empty workflow-empty-soft">
        {{ workflows.length ? "没有匹配的工作流" : "还没有阶段工作流" }}
      </div>

      <div v-else class="workflow-project-list">
        <article
          v-for="item in filteredWorkflows"
          :key="item.id"
          class="workflow-project-card"
          :class="{ 'workflow-project-card-active': item.id === selectedWorkflowId }"
        >
          <button type="button" class="workflow-project-card__open" @click="openWorkflow(item.id, workflowSummaryCanvasStage(item))">
            <div class="workflow-project-card__title">
              <strong>{{ item.title }}</strong>
              <span>{{ workflowStageLabel(workflowSummaryCanvasStage(item)) }}</span>
            </div>
            <div class="workflow-project-card__stats">
              <span>分镜 {{ item.storyboardVersionCount }}</span>
              <span>角色 {{ workflowSummaryCharacterCountLabel(item) }}</span>
              <span>关键帧 {{ item.keyframeVersionCount }}</span>
              <span>视频 {{ item.videoVersionCount }}</span>
            </div>
            <div class="workflow-project-card__meta">
              <span>{{ item.aspectRatio }}</span>
              <span>{{ item.status }}</span>
              <span>{{ formatDateTime(item.updatedAt) }}</span>
            </div>
          </button>
          <details class="workflow-more-menu">
            <summary aria-label="更多操作">•••</summary>
            <button
              type="button"
              class="workflow-menu-danger"
              :disabled="busyActionKey === `delete-workflow-${item.id}`"
              @click="handleDeleteWorkflow(item)"
            >
              {{ busyActionKey === `delete-workflow-${item.id}` ? "删除中..." : "删除工作流" }}
            </button>
          </details>
        </article>
      </div>
    </aside>

    <section class="workflow-canvas-main">
      <section v-if="createComposerVisible" class="workflow-create-board">
        <div class="workflow-create-composer">
          <div class="workflow-create-title">
            <p class="workflow-eyebrow">New Canvas</p>
            <h2>把正文放进画布，开始阶段创作</h2>
          </div>

          <form class="workflow-composer-card" @submit.prevent="handleCreateWorkflow">
            <label class="workflow-composer-title-field">
              <span>标题</span>
              <input v-model="createForm.title" required placeholder="例如：第 12 集阶段拆分版" />
            </label>
            <label class="workflow-composer-body-field">
              <span>正文</span>
              <textarea v-model="createForm.transcriptText" rows="10" placeholder="输入小说正文或脚本内容"></textarea>
            </label>
            <label class="workflow-composer-prompt-field">
              <span>全局 Prompt</span>
              <textarea v-model="createForm.globalPrompt" rows="4" placeholder="可选：补充整体风格、节奏、镜头要求"></textarea>
            </label>

            <div class="workflow-composer-toolbar">
              <span class="tool-pill tool-pill-accent">阶段画布</span>
              <span class="tool-pill">分镜</span>
              <span class="tool-pill">关键帧</span>
              <span class="tool-pill">视频</span>
              <span class="composer-required-count">必填 {{ createReviewConfiguredCount }} / {{ createReviewRequiredItems.length }}</span>
              <button
                class="workflow-composer-submit"
                type="submit"
                :disabled="creatingWorkflow || !canSubmitCreateReview"
                :title="canSubmitCreateReview ? '创建画布' : '请先补全必填项'"
              >
                <span v-if="creatingWorkflow">...</span>
                <span v-else>↑</span>
              </button>
            </div>
          </form>
        </div>

        <aside class="workflow-inspector workflow-create-inspector">
          <div class="inspector-section">
            <div class="inspector-section__head">
              <p class="workflow-eyebrow">Advanced</p>
              <h3>高级参数</h3>
            </div>
            <div class="workflow-settings-stack">
              <label class="workflow-field">
                <span>文本模型</span>
                <select v-model="createForm.textAnalysisModel" class="field-input">
                  <option v-for="item in textModelOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                </select>
              </label>
              <label class="workflow-field">
                <span>关键帧模型</span>
                <select v-model="createForm.imageModel" class="field-input">
                  <option v-for="item in imageModelOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                </select>
              </label>
              <label class="workflow-field">
                <span>视频模型</span>
                <select v-model="createForm.videoModel" class="field-input">
                  <option v-for="item in videoModelOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                </select>
              </label>
              <label class="workflow-field">
                <span>视觉风格</span>
                <select v-model="createForm.stylePreset" class="field-input">
                  <option v-for="item in stylePresetOptions" :key="item.key" :value="item.key">{{ item.label }}</option>
                </select>
              </label>
              <label class="workflow-field">
                <span>画幅</span>
                <select v-model="createForm.aspectRatio" class="field-input">
                  <option v-for="item in aspectRatioOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                </select>
              </label>
              <label class="workflow-field">
                <span>输出尺寸</span>
                <select v-model="createForm.videoSize" class="field-input">
                  <option v-for="item in videoSizeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                </select>
              </label>
              <div class="workflow-field">
                <span>镜头时长</span>
                <div class="stage-toggle-row">
                  <button type="button" class="stage-toggle-chip" :class="{ 'stage-toggle-chip-active': storyboardDurationMode === 'auto' }" @click="storyboardDurationMode = 'auto'">自动</button>
                  <button type="button" class="stage-toggle-chip" :class="{ 'stage-toggle-chip-active': storyboardDurationMode === 'manual' }" @click="storyboardDurationMode = 'manual'">手动</button>
                </div>
              </div>
              <label v-if="storyboardDurationMode === 'manual'" class="workflow-field">
                <span>固定秒数</span>
                <input v-model="storyboardManualDurationSeconds" class="field-input" type="number" :min="STORYBOARD_MANUAL_DURATION_MIN_SECONDS" :max="STORYBOARD_MANUAL_DURATION_MAX_SECONDS" step="1" />
              </label>
              <label class="workflow-field">
                <span>关键帧 Seed</span>
                <input v-model="createForm.keyframeSeed" class="field-input" type="number" min="0" placeholder="自动" />
              </label>
              <label class="workflow-field">
                <span>视频 Seed</span>
                <input v-model="createForm.videoSeed" class="field-input" type="number" min="0" placeholder="自动" />
              </label>
            </div>
            <p v-if="storyboardManualDurationValidationMessage" class="workflow-error">{{ storyboardManualDurationValidationMessage }}</p>
            <p v-if="createError" class="workflow-error">{{ createError }}</p>
            <div class="workflow-inspector-actions">
              <button class="btn-secondary btn-sm" type="button" :disabled="creatingWorkflow" @click="closeCreateReview">取消</button>
              <button class="btn-primary btn-sm" type="button" :disabled="creatingWorkflow || !canSubmitCreateReview" @click="handleCreateWorkflow">
                {{ creatingWorkflow ? "保存中..." : "创建画布" }}
              </button>
            </div>
          </div>
        </aside>
      </section>

      <div v-else-if="detailError" class="surface-panel workflow-banner workflow-banner-error">
        <p>{{ detailError }}</p>
        <button class="btn-secondary btn-sm" type="button" :disabled="loadingDetail" @click="reloadCurrentWorkflow">重新加载</button>
      </div>

      <div v-else-if="loadingDetail" class="surface-panel workflow-empty workflow-empty-large">
        正在加载工作流详情...
      </div>

      <template v-else-if="selectedWorkflow">
        <header class="workflow-canvas-header">
          <div>
            <p class="workflow-eyebrow">Creative Canvas</p>
            <h2>{{ selectedWorkflow.title }}</h2>
            <div class="workflow-summary__meta">
              <span class="surface-chip">{{ selectedWorkflow.status }}</span>
              <span class="surface-chip">{{ workflowStageLabel(workflowCanvasStageFromCurrent(selectedWorkflow)) }}</span>
              <span class="surface-chip">{{ selectedWorkflow.aspectRatio }}</span>
              <span class="surface-chip">评分 {{ ratingLabel(selectedWorkflow.effectRating) }}</span>
            </div>
          </div>
          <button class="btn-secondary btn-sm" type="button" @click="workflowSettingsOpen = !workflowSettingsOpen">
            {{ workflowSettingsOpen ? "收起参数" : "参数" }}
          </button>
        </header>

        <nav class="workflow-stage-pipeline" aria-label="阶段流水线">
          <button
            v-for="stage in canvasStageItems"
            :key="stage.key"
            type="button"
            class="workflow-stage-step"
            :class="{
              'workflow-stage-step-active': activeCanvasStage === stage.key,
              'workflow-stage-step-ready': stage.ready,
            }"
            @click="switchCanvasStage(stage.key)"
          >
            <span class="workflow-stage-step__index">{{ stage.index }}</span>
            <span class="workflow-stage-step__text">
              <strong>{{ stage.label }}</strong>
              <small>{{ stage.status }}</small>
            </span>
            <span class="workflow-stage-step__count">{{ stage.count }}</span>
          </button>
        </nav>

        <section class="workflow-canvas-grid">
          <main class="workflow-stage-canvas">
            <section v-if="activeCanvasStage === 'storyboard'" class="workflow-stage-board storyboard-board">
              <div class="stage-board__head">
                <div>
                  <p class="workflow-eyebrow">Stage 1</p>
                  <h3>分镜脚本</h3>
                </div>
                <button class="btn-primary btn-sm" type="button" :disabled="busyActionKey === 'storyboard'" @click="handleGenerateStoryboard">
                  {{ busyActionKey === "storyboard" ? "生成中..." : "生成分镜版本" }}
                </button>
              </div>

              <div v-if="!selectedWorkflow.storyboardVersions.length" class="workflow-empty workflow-empty-large">
                还没有分镜版本，先执行一次分镜生成。
              </div>
              <div v-else class="storyboard-layout">
                <article class="storyboard-preview-card">
                  <div class="storyboard-preview-card__head">
                    <div>
                      <span class="surface-chip">当前分镜</span>
                      <h4>{{ selectedStoryboardVersion?.title || "未选择分镜" }}</h4>
                    </div>
                    <button
                      v-if="selectedStoryboardVersion && !selectedStoryboardVersion.selected"
                      class="btn-secondary btn-sm"
                      type="button"
                      :disabled="busyActionKey === selectedStoryboardVersion.id"
                      @click="handleSelectStoryboard(selectedStoryboardVersion.id)"
                    >
                      {{ busyActionKey === selectedStoryboardVersion.id ? "处理中..." : "设为当前" }}
                    </button>
                  </div>
                  <div v-if="selectedStoryboardVersion" class="version-card__markdown storyboard-preview-markdown" v-html="storyboardPreviewHtml(selectedStoryboardVersion)"></div>
                </article>

                <aside class="version-side-panel">
                  <div class="version-side-panel__head">
                    <strong>版本历史</strong>
                    <span class="surface-chip">{{ selectedWorkflow.storyboardVersions.length }} 个版本</span>
                  </div>
                  <div class="compact-version-list">
                    <article
                      v-for="version in selectedWorkflow.storyboardVersions"
                      :key="version.id"
                      class="compact-version-card"
                      :class="{ 'compact-version-card-active': selectedStoryboardVersion?.id === version.id }"
                    >
                      <button type="button" class="compact-version-card__main" @click="setPreviewStoryboardVersion(version.id)">
                        <strong>V{{ version.versionNo }} · {{ version.title }}</strong>
                        <span>{{ version.status }} · 评分 {{ ratingLabel(version.rating) }}</span>
                      </button>
                      <details class="workflow-more-menu compact-version-menu">
                        <summary aria-label="版本操作">•••</summary>
                        <button type="button" :disabled="version.selected || busyActionKey === version.id" @click="handleSelectStoryboard(version.id)">
                          {{ version.selected ? "已选中" : "设为当前" }}
                        </button>
                        <button type="button" :disabled="!version.asset || busyActionKey === `reuse-${version.id}`" @click="handleReuseAsset(version.asset?.id || '', version.id)">复制为新工作流</button>
                        <button type="button" class="workflow-menu-danger" :disabled="busyActionKey === `delete-${version.id}`" @click="handleDeleteStageVersion(version)">删除版本</button>
                      </details>
                    </article>
                  </div>
                  <div v-if="selectedStoryboardVersion" class="storyboard-adjust-panel">
                    <input v-model="storyboardAdjustmentDrafts[selectedStoryboardVersion.id]" class="field-input" type="text" placeholder="输入调整要求，留空则自动审查" />
                    <button
                      class="btn-primary btn-sm"
                      type="button"
                      :disabled="busyActionKey === `storyboard-adjust-${selectedStoryboardVersion.id}` || selectedStoryboardVersion.status !== 'SUCCEEDED'"
                      @click="handleAdjustStoryboard(selectedStoryboardVersion.id)"
                    >
                      {{ busyActionKey === `storyboard-adjust-${selectedStoryboardVersion.id}` ? "调整中..." : "调整分镜" }}
                    </button>
                  </div>
                </aside>
              </div>
            </section>

            <section v-else-if="activeCanvasStage === 'character'" class="workflow-stage-board character-board">
              <div class="stage-board__head">
                <div>
                  <p class="workflow-eyebrow">Stage 2</p>
                  <h3>角色三视图</h3>
                </div>
                <button class="btn-primary btn-sm" type="button" :disabled="!missingCharacterSheets.length || busyActionKey === 'character-missing'" @click="handleGenerateMissingCharacterSheets">
                  {{ busyActionKey === "character-missing" ? "生成中..." : "生成缺失角色" }}
                </button>
              </div>

              <section class="character-strip">
                <div class="character-strip__head">
                  <strong>角色三视图</strong>
                  <span class="surface-chip">{{ workflowCharacterSheets.length }} 个角色</span>
                </div>
                <div v-if="!workflowCharacterSheets.length" class="workflow-empty workflow-empty-nested">当前工作流还没有角色三视图。</div>
                <div v-else class="character-strip__list">
                  <article v-for="sheet in workflowCharacterSheets" :key="characterSheetKey(sheet)" class="character-mini-card">
                    <div>
                      <strong>{{ characterSheetTitle(sheet) }}</strong>
                      <p>{{ characterSheetAppearanceSummary(sheet) }}</p>
                    </div>
                    <div v-if="selectedCharacterSheetVersion(sheet)" class="character-mini-card__frames">
                      <button
                        v-for="frame in characterSheetPreviewFrames(selectedCharacterSheetVersion(sheet)!)"
                        :key="`${characterSheetKey(sheet)}-${frame.role}`"
                        type="button"
                        class="character-mini-frame"
                        :aria-label="`查看${characterSheetTitle(sheet)}${frame.label}`"
                        @click="openImagePreview(frame.url, `${characterSheetTitle(sheet)} ${frame.label}`)"
                      >
                        <img :src="frame.url" :alt="`${characterSheetTitle(sheet)} ${frame.label}`" />
                        <span>{{ frame.label }}</span>
                      </button>
                    </div>
                    <div class="character-mini-card__actions">
                      <button class="btn-secondary btn-sm" type="button" :disabled="characterSheetClipIndex(sheet) === null" @click="openCharacterAssetPicker(sheet)">素材库</button>
                      <button class="btn-ghost btn-sm" type="button" :disabled="characterSheetClipIndex(sheet) === null || busyActionKey === 'character-missing' || busyActionKey === `keyframe-${characterSheetClipIndex(sheet)}`" @click="handleGenerateKeyframe(characterSheetClipIndex(sheet) || 0)">
                        {{ selectedCharacterSheetVersion(sheet) ? "重生" : "生成" }}
                      </button>
                    </div>
                    <section v-if="isCharacterAssetPickerOpen(sheet)" class="character-asset-picker">
                      <div class="character-asset-picker__head">
                        <div>
                          <p class="workflow-eyebrow">Material Library</p>
                          <h4>选择 {{ characterSheetTitle(sheet) }} 的三视图素材</h4>
                        </div>
                        <button class="btn-secondary btn-sm" type="button" @click="closeCharacterAssetPicker">收起</button>
                      </div>
                      <div class="character-asset-picker__filters">
                        <label class="workflow-field">
                          <span>关键词</span>
                          <input v-model="characterAssetPicker.keyword" class="field-input" type="search" placeholder="按角色名、标题搜索" @keyup.enter="loadCharacterAssetCandidates(sheet)" />
                        </label>
                        <label class="workflow-field">
                          <span>模型</span>
                          <input v-model="characterAssetPicker.model" class="field-input" type="search" placeholder="按模型筛选" @keyup.enter="loadCharacterAssetCandidates(sheet)" />
                        </label>
                        <button class="btn-secondary btn-sm character-asset-picker__search" type="button" :disabled="characterAssetPicker.loading" @click="loadCharacterAssetCandidates(sheet)">
                          {{ characterAssetPicker.loading ? "搜索中..." : "搜索素材" }}
                        </button>
                      </div>
                      <p v-if="characterAssetPicker.error" class="workflow-error">{{ characterAssetPicker.error }}</p>
                      <div v-else-if="characterAssetPicker.loading" class="workflow-empty workflow-empty-nested">正在加载三视图素材...</div>
                      <div v-else-if="!characterAssetPicker.assets.length" class="workflow-empty workflow-empty-nested">没有匹配的角色三视图素材。</div>
                      <div v-else class="character-asset-grid">
                        <article v-for="asset in characterAssetPicker.assets" :key="asset.id" class="character-asset-card">
                          <button type="button" class="character-asset-card__preview" :aria-label="`查看${asset.title}素材预览`" @click="openImagePreview(materialAssetPreviewUrl(asset), asset.title)">
                            <img :src="materialAssetPreviewUrl(asset)" :alt="asset.title" />
                          </button>
                          <div class="character-asset-card__body">
                            <strong>{{ asset.title }}</strong>
                            <div class="character-asset-card__meta">
                              <span class="surface-chip surface-chip-quiet">{{ materialAssetModelLabel(asset) }}</span>
                              <span class="surface-chip surface-chip-quiet">评分 {{ ratingLabel(asset.userRating) }}</span>
                            </div>
                          </div>
                          <button class="btn-primary btn-sm" type="button" :disabled="busyActionKey === `character-sheet-asset-${characterSheetClipIndex(sheet)}`" @click="handleSelectCharacterSheetAsset(sheet, asset.id)">
                            {{ busyActionKey === `character-sheet-asset-${characterSheetClipIndex(sheet)}` ? "选择中..." : "选择此素材" }}
                          </button>
                        </article>
                      </div>
                    </section>
                  </article>
                </div>
              </section>
            </section>

            <section v-else-if="activeCanvasStage === 'keyframe'" class="workflow-stage-board keyframe-board">
              <div class="stage-board__head">
                <div>
                  <p class="workflow-eyebrow">Stage 3</p>
                  <h3>关键帧</h3>
                </div>
                <button class="btn-primary btn-sm" type="button" :disabled="!selectedCanvasClip || busyActionKey === `keyframe-${selectedCanvasClip.clipIndex}`" @click="selectedCanvasClip && handleGenerateKeyframe(selectedCanvasClip.clipIndex)">
                  {{ selectedCanvasClip && busyActionKey === `keyframe-${selectedCanvasClip.clipIndex}` ? "生成中..." : "生成当前镜头关键帧" }}
                </button>
              </div>

              <section class="clip-workbench">
                <nav class="clip-timeline" aria-label="镜头列表">
                  <button
                    v-for="slot in selectedWorkflow.clipSlots"
                    :key="slot.clipIndex"
                    type="button"
                    class="clip-timeline__item"
                    :class="{ 'clip-timeline__item-active': selectedCanvasClip?.clipIndex === slot.clipIndex }"
                    @click="selectCanvasClip(slot.clipIndex)"
                  >
                    <strong>{{ slot.shotLabel || `镜头 #${slot.clipIndex}` }}</strong>
                    <span>{{ slot.keyframeVersions.length ? `${slot.keyframeVersions.length} 版` : '未生成' }}</span>
                  </button>
                </nav>

                <article v-if="selectedCanvasClip" class="clip-detail-card">
                  <div class="clip-detail-card__head">
                    <div>
                      <p class="workflow-eyebrow">Clip {{ selectedCanvasClip.clipIndex }}</p>
                      <h4>{{ selectedCanvasClip.shotLabel || `镜头 #${selectedCanvasClip.clipIndex}` }}</h4>
                      <p>{{ clipSceneSummary(selectedCanvasClip) }}</p>
                    </div>
                    <span class="surface-chip">{{ selectedCanvasClip.durationHint || `${selectedCanvasClip.targetDurationSeconds || 0}s` }}</span>
                  </div>
                  <div v-if="selectedKeyframeVersion(selectedCanvasClip)" class="keyframe-frame-grid">
                    <article v-for="frame in keyframePreviewFrames(selectedKeyframeVersion(selectedCanvasClip)!, selectedCanvasClip)" :key="`${selectedCanvasClip.clipIndex}-${frame.role}`" class="keyframe-frame-card">
                      <div class="keyframe-frame-card__head">
                        <span class="surface-chip surface-chip-quiet">{{ frame.label }}</span>
                        <span v-if="frame.selected" class="surface-chip">已选中</span>
                      </div>
                      <button v-if="frame.url" type="button" class="character-sheet-preview-trigger" :aria-label="`查看${frame.label}原图`" @click="openKeyframeImagePreview(selectedKeyframeVersion(selectedCanvasClip)!, frame)">
                        <img class="version-card__image keyframe-frame-card__image" :src="frame.url" :alt="frame.label" />
                      </button>
                      <div v-else class="keyframe-frame-card__failure">
                        <strong>{{ frame.label }}生成失败</strong>
                        <span>{{ frame.errorMessage || "请单独重生此帧" }}</span>
                      </div>
                      <div class="keyframe-frame-card__actions">
                        <button class="btn-secondary btn-sm" type="button" :disabled="frame.selected || busyActionKey === `${selectedKeyframeVersion(selectedCanvasClip)!.id}-${frame.role}`" @click="handleSelectKeyframeFrame(selectedCanvasClip.clipIndex, selectedKeyframeVersion(selectedCanvasClip)!.id, frame.role)">选中此帧</button>
                        <button class="btn-ghost btn-sm" type="button" :disabled="!frame.regenerable || busyActionKey === `keyframe-${selectedCanvasClip.clipIndex}-${frame.role}`" @click="handleGenerateKeyframeFrame(selectedCanvasClip.clipIndex, frame.role)">重生此帧</button>
                      </div>
                    </article>
                  </div>
                  <div v-else class="workflow-empty workflow-empty-nested">当前镜头还没有选中的关键帧。</div>

                  <div class="compact-version-list horizontal-version-list">
                    <article v-for="version in selectedCanvasClip.keyframeVersions" :key="version.id" class="compact-version-card">
                      <button type="button" class="compact-version-card__main" @click="handleSelectKeyframe(selectedCanvasClip.clipIndex, version.id)">
                        <strong>V{{ version.versionNo }} · {{ version.title }}</strong>
                        <span>{{ version.status }} · Seed {{ seedLabel(versionSeed(version) ?? selectedWorkflow.keyframeSeed) }}</span>
                      </button>
                      <details class="workflow-more-menu compact-version-menu">
                        <summary aria-label="版本操作">•••</summary>
                        <button type="button" :disabled="version.selected || busyActionKey === version.id" @click="handleSelectKeyframe(selectedCanvasClip.clipIndex, version.id)">选中继续</button>
                        <button type="button" :disabled="!version.asset || busyActionKey === `reuse-${version.id}`" @click="handleReuseAsset(version.asset?.id || '', version.id)">复用</button>
                        <button type="button" class="workflow-menu-danger" :disabled="busyActionKey === `delete-${version.id}`" @click="handleDeleteStageVersion(version)">删除版本</button>
                      </details>
                    </article>
                  </div>
                </article>
                <div v-else class="workflow-empty workflow-empty-large">选中分镜版本后，这里会展开镜头列表。</div>
              </section>
            </section>

            <section v-else-if="activeCanvasStage === 'video'" class="workflow-stage-board video-board">
              <div class="stage-board__head">
                <div>
                  <p class="workflow-eyebrow">Stage 4</p>
                  <h3>视频片段</h3>
                </div>
                <button class="btn-primary btn-sm" type="button" :disabled="!selectedCanvasClip || !selectedKeyframeVersion(selectedCanvasClip) || busyActionKey === `video-${selectedCanvasClip.clipIndex}`" @click="selectedCanvasClip && handleGenerateVideo(selectedCanvasClip.clipIndex)">
                  {{ selectedCanvasClip && busyActionKey === `video-${selectedCanvasClip.clipIndex}` ? "生成中..." : "生成当前镜头视频" }}
                </button>
              </div>

              <div class="readiness-strip">
                <span>总镜头 {{ videoReadiness.total }}</span>
                <span>已生成 {{ videoReadiness.generated }}</span>
                <span>已选中 {{ videoReadiness.selected }}</span>
                <span>{{ canFinalize ? "可拼接" : `还差 ${videoReadiness.missing.length} 个镜头` }}</span>
              </div>

              <section class="clip-workbench">
                <nav class="clip-timeline" aria-label="视频镜头列表">
                  <button
                    v-for="slot in selectedWorkflow.clipSlots"
                    :key="`video-tab-${slot.clipIndex}`"
                    type="button"
                    class="clip-timeline__item"
                    :class="{ 'clip-timeline__item-active': selectedCanvasClip?.clipIndex === slot.clipIndex }"
                    @click="selectCanvasClip(slot.clipIndex)"
                  >
                    <strong>{{ slot.shotLabel || `镜头 #${slot.clipIndex}` }}</strong>
                    <span>{{ slot.videoVersions.some((version) => version.selected) ? '已选中' : (slot.videoVersions.length ? '待选择' : '未生成') }}</span>
                  </button>
                </nav>

                <article v-if="selectedCanvasClip" class="clip-detail-card video-clip-detail">
                  <div class="clip-detail-card__head">
                    <div>
                      <p class="workflow-eyebrow">Clip {{ selectedCanvasClip.clipIndex }}</p>
                      <h4>{{ selectedCanvasClip.shotLabel || `镜头 #${selectedCanvasClip.clipIndex}` }}</h4>
                      <p>{{ clipSceneSummary(selectedCanvasClip) }}</p>
                    </div>
                    <span class="surface-chip">{{ selectedKeyframeVersion(selectedCanvasClip)?.title || "未选择关键帧" }}</span>
                  </div>

                  <div v-if="selectedKeyframePreviewFrames(selectedCanvasClip).length" class="keyframe-thumb-list">
                    <article v-for="frame in selectedKeyframePreviewFrames(selectedCanvasClip)" :key="`video-input-${selectedCanvasClip.clipIndex}-${frame.role}`" class="keyframe-thumb-card">
                      <img class="keyframe-thumb-card__image" :src="frame.url" :alt="frame.label" />
                      <span class="surface-chip surface-chip-quiet">{{ frame.label }}</span>
                    </article>
                  </div>

                  <div v-if="!selectedCanvasClip.videoVersions.length" class="workflow-empty workflow-empty-nested">还没有视频版本。</div>
                  <div v-else class="video-version-grid">
                    <article v-for="version in selectedCanvasClip.videoVersions" :key="version.id" class="video-version-card" :class="{ 'video-version-card-active': version.selected }">
                      <div class="video-version-card__head">
                        <strong>{{ version.title }}</strong>
                        <span v-if="version.selected" class="surface-chip">当前选中</span>
                      </div>
                      <video v-if="version.previewUrl" class="version-card__video" :src="version.previewUrl" controls playsinline preload="metadata"></video>
                      <div class="version-card__actions">
                        <button class="btn-secondary btn-sm" type="button" :disabled="version.selected || busyActionKey === version.id" @click="handleSelectVideo(selectedCanvasClip.clipIndex, version.id)">选中继续</button>
                        <details class="workflow-more-menu compact-version-menu">
                          <summary aria-label="版本操作">•••</summary>
                          <button type="button" :disabled="!version.asset || busyActionKey === `reuse-${version.id}`" @click="handleReuseAsset(version.asset?.id || '', version.id)">复用</button>
                          <a v-if="version.downloadUrl" :href="version.downloadUrl" download target="_blank" rel="noopener noreferrer">下载</a>
                          <button type="button" class="workflow-menu-danger" :disabled="busyActionKey === `delete-${version.id}`" @click="handleDeleteStageVersion(version)">删除版本</button>
                        </details>
                      </div>
                    </article>
                  </div>
                </article>
              </section>
            </section>

            <section v-else class="workflow-stage-board final-board">
              <div class="stage-board__head">
                <div>
                  <p class="workflow-eyebrow">Final</p>
                  <h3>成片</h3>
                </div>
                <button class="btn-primary btn-sm" type="button" :disabled="!canFinalize || busyActionKey === 'finalize'" @click="handleFinalize">
                  {{ busyActionKey === "finalize" ? "拼接中..." : finalizeButtonLabel }}
                </button>
              </div>

              <div class="readiness-strip readiness-strip-final">
                <span>总镜头 {{ videoReadiness.total }}</span>
                <span>已选中 {{ videoReadiness.selected }}</span>
                <span>{{ finalizeHint }}</span>
              </div>

              <article v-if="selectedWorkflow.finalResult" class="final-result-card-v2">
                <video v-if="selectedWorkflow.finalResult.previewUrl" class="final-result-card__video" :src="selectedWorkflow.finalResult.previewUrl" controls playsinline preload="metadata"></video>
                <div class="final-result-card-v2__meta">
                  <h4>{{ selectedWorkflow.finalResult.title }}</h4>
                  <div class="workflow-kv">
                    <div class="workflow-kv__row"><span>时长</span><strong>{{ durationLabel(selectedWorkflow.finalResult.durationSeconds) }}</strong></div>
                    <div class="workflow-kv__row"><span>评分</span><strong>{{ ratingLabel(selectedWorkflow.effectRating) }}</strong></div>
                  </div>
                  <a class="btn-primary btn-sm" :href="selectedWorkflow.finalResult.fileUrl" download target="_blank" rel="noopener noreferrer">下载结果视频</a>
                </div>
              </article>
              <div v-else class="workflow-empty workflow-empty-large">
                {{ canFinalize ? "可以拼接完整视频。" : `还有 ${videoReadiness.missing.length} 个镜头未选中视频版本。` }}
              </div>

              <section v-if="videoReadiness.missing.length" class="missing-clip-list">
                <strong>阻塞镜头</strong>
                <button v-for="slot in videoReadiness.missing" :key="`missing-${slot.clipIndex}`" type="button" class="missing-clip-card" @click="selectCanvasClip(slot.clipIndex); switchCanvasStage('video')">
                  {{ slot.shotLabel || `镜头 #${slot.clipIndex}` }}
                </button>
              </section>
            </section>
          </main>

          <aside class="workflow-inspector">
            <section class="inspector-section">
              <div class="inspector-section__head">
                <p class="workflow-eyebrow">Inspector</p>
                <h3>参数与操作</h3>
              </div>
              <div class="inspector-kv-list">
                <div><span>文本模型</span><strong>{{ valueOptionLabel(textModelOptions, selectedWorkflow.textAnalysisModel, selectedWorkflow.textAnalysisModel) }}</strong></div>
                <div><span>关键帧模型</span><strong>{{ valueOptionLabel(imageModelOptions, selectedWorkflow.imageModel, selectedWorkflow.imageModel) }}</strong></div>
                <div><span>视频模型</span><strong>{{ valueOptionLabel(videoModelOptions, selectedWorkflow.videoModel, selectedWorkflow.videoModel) }}</strong></div>
                <div><span>画幅</span><strong>{{ selectedWorkflow.aspectRatio }}</strong></div>
                <div><span>尺寸</span><strong>{{ valueOptionLabel(catalogVideoSizeOptions, selectedWorkflow.videoSize, selectedWorkflow.videoSize || '未设置') }}</strong></div>
                <div><span>关键帧 Seed</span><strong>{{ seedLabel(selectedWorkflow.keyframeSeed) }}</strong></div>
                <div><span>视频 Seed</span><strong>{{ seedLabel(selectedWorkflow.videoSeed) }}</strong></div>
              </div>
            </section>

            <section v-if="workflowSettingsOpen" class="inspector-section inspector-settings-form">
              <div class="inspector-section__head">
                <p class="workflow-eyebrow">Settings</p>
                <h3>编辑参数</h3>
              </div>
              <form class="workflow-settings-stack" @submit.prevent="handleUpdateWorkflowSettings">
                <label class="workflow-field"><span>文本模型</span><select v-model="workflowSettingsDraft.textAnalysisModel" class="field-input"><option v-for="item in textModelOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
                <label class="workflow-field"><span>关键帧模型</span><select v-model="workflowSettingsDraft.imageModel" class="field-input"><option v-for="item in imageModelOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
                <label class="workflow-field"><span>视频模型</span><select v-model="workflowSettingsDraft.videoModel" class="field-input"><option v-for="item in videoModelOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
                <label class="workflow-field"><span>视觉风格</span><select v-model="workflowSettingsDraft.stylePreset" class="field-input"><option v-for="item in stylePresetOptions" :key="item.key" :value="item.key">{{ item.label }}</option></select></label>
                <label class="workflow-field"><span>长宽比</span><select v-model="workflowSettingsDraft.aspectRatio" class="field-input"><option v-for="item in aspectRatioOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
                <label class="workflow-field"><span>输出尺寸</span><select v-model="workflowSettingsDraft.videoSize" class="field-input"><option v-for="item in workflowSettingsVideoSizeOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
                <label class="workflow-field"><span>关键帧 Seed</span><input v-model="workflowSettingsDraft.keyframeSeed" class="field-input" type="number" min="0" placeholder="自动" /></label>
                <label class="workflow-field"><span>视频 Seed</span><input v-model="workflowSettingsDraft.videoSeed" class="field-input" type="number" min="0" placeholder="自动" /></label>
                <div class="workflow-field"><span>镜头时长</span><div class="stage-toggle-row"><button type="button" class="stage-toggle-chip" :class="{ 'stage-toggle-chip-active': workflowSettingsDraft.durationMode === 'auto' }" @click="workflowSettingsDraft.durationMode = 'auto'">自动</button><button type="button" class="stage-toggle-chip" :class="{ 'stage-toggle-chip-active': workflowSettingsDraft.durationMode === 'manual' }" @click="workflowSettingsDraft.durationMode = 'manual'">手动</button></div></div>
                <label class="workflow-field"><span>最小时长</span><input v-model="workflowSettingsDraft.minDurationSeconds" class="field-input" type="number" min="1" max="60" step="1" :disabled="workflowSettingsDraft.durationMode === 'auto'" /></label>
                <label class="workflow-field"><span>最大时长</span><input v-model="workflowSettingsDraft.maxDurationSeconds" class="field-input" type="number" min="1" max="60" step="1" :disabled="workflowSettingsDraft.durationMode === 'auto'" /></label>
                <p v-if="workflowSettingsValidationMessage" class="workflow-error">{{ workflowSettingsValidationMessage }}</p>
                <button class="btn-primary btn-sm" type="submit" :disabled="busyActionKey === 'workflow-settings' || Boolean(workflowSettingsValidationMessage)">
                  {{ busyActionKey === "workflow-settings" ? "保存中..." : "保存设置" }}
                </button>
              </form>
            </section>

            <section v-if="selectedStageVersion" class="inspector-section">
              <div class="inspector-section__head">
                <p class="workflow-eyebrow">Version</p>
                <h3>当前版本评价</h3>
              </div>
              <div class="inspector-kv-list">
                <div><span>版本</span><strong>{{ selectedStageVersion.title }}</strong></div>
                <div><span>状态</span><strong>{{ selectedStageVersion.status }}</strong></div>
                <div><span>评分</span><strong>{{ ratingLabel(selectedStageVersion.rating) }}</strong></div>
              </div>
              <div class="rating-row">
                <button
                  v-for="score in ratingOptions"
                  :key="`${selectedStageVersion.id}-${score}`"
                  type="button"
                  class="rating-pill"
                  :class="{ 'rating-pill-active': Number(stageRatingDrafts[selectedStageVersion.id] || selectedStageVersion.rating || 0) === score }"
                  @click="setStageRatingDraft(selectedStageVersion.id, score)"
                >
                  {{ score }}
                </button>
              </div>
              <textarea v-model="stageNoteDrafts[selectedStageVersion.id]" class="field-textarea" rows="3" placeholder="记录当前版本评价"></textarea>
              <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === `rate-${selectedStageVersion.id}`" @click="handleRateStageVersion(selectedStageVersion)">
                {{ busyActionKey === `rate-${selectedStageVersion.id}` ? "保存中..." : "保存版本评分" }}
              </button>
            </section>

            <section class="inspector-section">
              <div class="inspector-section__head">
                <p class="workflow-eyebrow">Rating</p>
                <h3>当前工作流评价</h3>
              </div>
              <div class="rating-row">
                <button v-for="score in ratingOptions" :key="score" type="button" class="rating-pill" :class="{ 'rating-pill-active': Number(workflowRatingDraft) === score }" @click="workflowRatingDraft = String(score)">
                  {{ score }}
                </button>
              </div>
              <textarea v-model="workflowRatingNoteDraft" class="field-textarea" rows="3" placeholder="记录最终成片评价"></textarea>
              <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === 'workflow-rating'" @click="handleRateWorkflow">
                {{ busyActionKey === "workflow-rating" ? "保存中..." : "保存评分" }}
              </button>
            </section>
          </aside>
        </section>
      </template>

      <section v-else class="surface-panel workflow-panel workflow-empty-large workflow-empty-prompt">
        <h2>请新建画布或选择一个工作流来查看</h2>
        <div class="workflow-empty-prompt__actions">
          <button class="btn-primary btn-sm" type="button" :disabled="creatingWorkflow || loadingOptions" @click="startCreateWorkflow">新建画布</button>
        </div>
      </section>
    </section>
  </section>

  <div v-if="imagePreviewState.open" class="image-preview-overlay" role="dialog" aria-modal="true" @click.self="closeImagePreview">
    <div class="image-preview-caption">
      <strong>{{ imagePreviewCaption }}</strong>
      <span v-if="imagePreviewState.gallery.length > 1">按 ← / → 切换首尾帧</span>
    </div>
    <button type="button" class="image-preview-close" aria-label="关闭原图预览" @click="closeImagePreview">关闭</button>
    <img class="image-preview-full" :src="imagePreviewState.url" :alt="imagePreviewState.alt" />
  </div>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchGenerationOptions } from "@/api/generation";
import { fetchMaterialAssets, reuseMaterialAsset } from "@/api/material-assets";
import { requireAuth } from "@/auth/modal";
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
type DetailRouteStageKey = CreateStageKey | "character";
type CanvasStageKey = DetailRouteStageKey | "final";

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
const activeCreateStage = ref<DetailRouteStageKey>("storyboard");
const activeCanvasStage = ref<CanvasStageKey>("storyboard");
const createComposerVisible = ref(false);
const previewStoryboardVersionId = ref("");
const selectedCanvasClipIndex = ref<number | null>(null);

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

const detailStageKeys: DetailRouteStageKey[] = ["storyboard", "character", "keyframe", "video"];

const workflowRatingDraft = ref("5");
const workflowRatingNoteDraft = ref("");
const stageRatingDrafts = reactive<Record<string, string>>({});
const stageNoteDrafts = reactive<Record<string, string>>({});
const storyboardAdjustmentDrafts = reactive<Record<string, string>>({});
const workflowSettingsOpen = ref(false);
const workflowSettingsDraft = reactive({
  aspectRatio: "16:9",
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
  aspectRatio: "16:9",
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

function normalizeDetailStage(value: unknown): DetailRouteStageKey | null {
  const rawValue = Array.isArray(value) ? value[0] : value;
  if (typeof rawValue !== "string") {
    return null;
  }
  const normalizedValue = rawValue.trim().toLowerCase();
  if (normalizedValue === "joined") {
    return "video";
  }
  return detailStageKeys.includes(normalizedValue as DetailRouteStageKey) ? normalizedValue as DetailRouteStageKey : null;
}

function normalizeCanvasStage(value: unknown): CanvasStageKey | null {
  const rawValue = Array.isArray(value) ? value[0] : value;
  if (typeof rawValue !== "string") {
    return null;
  }
  const normalizedValue = rawValue.trim().toLowerCase();
  if (normalizedValue === "joined" || normalizedValue === "final") {
    return "final";
  }
  return normalizeDetailStage(normalizedValue);
}

const workflowCharacterSheets = computed(() => selectedWorkflow.value?.characterSheets ?? []);
const missingCharacterSheets = computed(() =>
  workflowCharacterSheets.value.filter((sheet) => !selectedCharacterSheetVersion(sheet))
);
const imagePreviewCaption = computed(() => imagePreviewState.caption || imagePreviewState.alt || "图片预览");
const selectedStoryboardVersion = computed(() => {
  const versions = selectedWorkflow.value?.storyboardVersions ?? [];
  if (!versions.length) {
    return null;
  }
  return (
    versions.find((version) => version.id === previewStoryboardVersionId.value)
    ?? versions.find((version) => version.selected)
    ?? versions[0]
  );
});
const selectedCanvasClip = computed(() => {
  const slots = selectedWorkflow.value?.clipSlots ?? [];
  if (!slots.length) {
    return null;
  }
  return slots.find((slot) => slot.clipIndex === selectedCanvasClipIndex.value) ?? slots[0];
});
const selectedVideoVersion = computed(() => selectedCanvasClip.value?.videoVersions.find((version) => version.selected) ?? selectedCanvasClip.value?.videoVersions[0] ?? null);
const selectedStageVersion = computed(() => {
  if (activeCanvasStage.value === "storyboard") {
    return selectedStoryboardVersion.value;
  }
  if (activeCanvasStage.value === "character") {
    const sheet = workflowCharacterSheets.value.find((item) => Boolean(selectedCharacterSheetVersion(item))) ?? workflowCharacterSheets.value[0];
    return sheet ? selectedCharacterSheetVersion(sheet) ?? characterSheetVersions(sheet)[0] ?? null : null;
  }
  if (activeCanvasStage.value === "keyframe") {
    const clip = selectedCanvasClip.value;
    return clip ? selectedKeyframeVersion(clip) ?? clip.keyframeVersions[0] ?? null : null;
  }
  if (activeCanvasStage.value === "video") {
    return selectedVideoVersion.value;
  }
  return null;
});
const videoReadiness = computed(() => {
  const slots = selectedWorkflow.value?.clipSlots ?? [];
  return {
    total: slots.length,
    generated: slots.filter((slot) => slot.videoVersions.length > 0).length,
    selected: slots.filter((slot) => slot.videoVersions.some((version) => version.selected)).length,
    missing: slots.filter((slot) => !slot.videoVersions.some((version) => version.selected)),
  };
});
const canvasStageItems = computed(() => {
  const workflow = selectedWorkflow.value;
  const storyboardCount = workflow?.storyboardVersions.length ?? 0;
  const keyframeCount = workflow?.clipSlots.reduce((sum, slot) => sum + slot.keyframeVersions.length, 0) ?? 0;
  const videoCount = workflow?.clipSlots.reduce((sum, slot) => sum + slot.videoVersions.length, 0) ?? 0;
  const selectedCharacterCount = workflowCharacterSheets.value.filter((sheet) => Boolean(selectedCharacterSheetVersion(sheet))).length;
  return [
    {
      key: "storyboard" as const,
      index: 1,
      label: "分镜脚本",
      status: storyboardCount ? "已有版本" : "待生成",
      count: `${storyboardCount} 版`,
      ready: storyboardCount > 0,
    },
    {
      key: "character" as const,
      index: 2,
      label: "角色三视图",
      status: selectedCharacterCount ? "已有角色" : (storyboardCount ? "可生成" : "等分镜"),
      count: `${selectedCharacterCount}/${workflowCharacterSheets.value.length || 0}`,
      ready: selectedCharacterCount > 0,
    },
    {
      key: "keyframe" as const,
      index: 3,
      label: "关键帧",
      status: keyframeCount ? "已有关键帧" : (storyboardCount ? "可生成" : "等角色"),
      count: `${keyframeCount} 版`,
      ready: keyframeCount > 0,
    },
    {
      key: "video" as const,
      index: 4,
      label: "视频片段",
      status: videoCount ? "已有视频" : (keyframeCount ? "可生成" : "等关键帧"),
      count: `${videoCount} 版`,
      ready: videoCount > 0,
    },
    {
      key: "final" as const,
      index: 5,
      label: "成片",
      status: workflow?.finalResult ? "已拼接" : (canFinalize.value ? "可拼接" : "未就绪"),
      count: workflow?.finalResult ? "已完成" : `${videoReadiness.value.selected}/${videoReadiness.value.total || 0}`,
      ready: Boolean(workflow?.finalResult || canFinalize.value),
    },
  ];
});

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

function workflowStageLabel(value?: string | null) {
  const normalized = String(value ?? "").trim().toLowerCase();
  if (normalized === "storyboard") {
    return "分镜脚本";
  }
  if (normalized === "character") {
    return "角色三视图";
  }
  if (normalized === "keyframe") {
    return "关键帧";
  }
  if (normalized === "video") {
    return "视频片段";
  }
  if (normalized === "joined" || normalized === "final") {
    return "成片";
  }
  if (normalized === "material_center") {
    return "素材中心";
  }
  return value || "未开始";
}

function workflowCanvasStageFromCurrent(workflow: WorkflowDetail): CanvasStageKey {
  const normalizedStage = normalizeCanvasStage(workflow.currentStage);
  if (normalizedStage === "keyframe" && hasMissingCharacterSheets(workflow)) {
    return "character";
  }
  return normalizedStage ?? "storyboard";
}

function workflowSummaryCanvasStage(workflow: WorkflowSummary): CanvasStageKey {
  const normalizedStage = normalizeCanvasStage(workflow.currentStage);
  if (normalizedStage === "keyframe") {
    const characterTotal = Number(workflow.characterSheetCount ?? 0);
    const selectedCharacterCount = Number(workflow.selectedCharacterSheetCount ?? 0);
    if (characterTotal > 0 && selectedCharacterCount < characterTotal) {
      return "character";
    }
  }
  return normalizedStage ?? "storyboard";
}

function workflowSummaryCharacterCountLabel(workflow: WorkflowSummary) {
  const characterTotal = Number(workflow.characterSheetCount ?? 0);
  const selectedCharacterCount = Number(workflow.selectedCharacterSheetCount ?? workflow.characterSheetVersionCount ?? 0);
  if (Number.isFinite(characterTotal) && characterTotal > 0) {
    return `${selectedCharacterCount}/${characterTotal}`;
  }
  return Number.isFinite(selectedCharacterCount) ? String(selectedCharacterCount) : "0";
}

function switchCanvasStage(stage: CanvasStageKey) {
  activeCanvasStage.value = stage;
  if (stage === "final") {
    return;
  }
  switchWorkflowStage(stage);
}

function setPreviewStoryboardVersion(versionId: string) {
  previewStoryboardVersionId.value = versionId;
}

function selectCanvasClip(clipIndex: number) {
  selectedCanvasClipIndex.value = clipIndex;
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

function hasMissingCharacterSheets(workflow: WorkflowDetail) {
  const sheets = workflow.characterSheets ?? [];
  return sheets.some((sheet) => !selectedCharacterSheetVersion(sheet));
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
  const authenticated = await requireAuth({
    title: "登录后选择素材",
    message: "素材库只展示你的个人素材，请先登录或使用邀请码注册。",
  });
  if (!authenticated) {
    detailError.value = "登录后可继续选择素材。";
    return;
  }
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
  const authenticated = await requireAuth({
    title: "登录后搜索素材",
    message: "素材库只展示你的个人素材，请先登录或使用邀请码注册。",
  });
  if (!authenticated) {
    characterAssetPicker.error = "登录后可搜索素材库。";
    return;
  }
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

async function closeCreateReview() {
  createError.value = "";
  createComposerVisible.value = false;
  selectedWorkflow.value = null;
  if (selectedWorkflowId.value) {
    await router.push("/workflows");
  }
}

function startCreateWorkflow() {
  createError.value = "";
  createComposerVisible.value = true;
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
  const nextStage = normalizeCanvasStage(preferredStage) ?? normalizeDetailStage(route.query.stage) ?? activeCreateStage.value;
  activeCanvasStage.value = nextStage;
  if (nextStage === "final") {
    void router.push(`/workflows/${workflowId}`);
    return;
  }
  void router.push({
    path: `/workflows/${workflowId}`,
    query: { stage: nextStage },
  });
}

function switchWorkflowStage(stage: DetailRouteStageKey) {
  activeCreateStage.value = stage;
  activeCanvasStage.value = stage;
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
      createForm.aspectRatio = (result.defaultAspectRatio as "9:16" | "16:9" | null) || "16:9";
    }
  } finally {
    loadingOptions.value = false;
  }
}

async function loadWorkflows() {
  const authenticated = await requireAuth({
    title: "登录后查看工作流",
    message: "阶段工作流只展示你的个人数据，请先登录或使用邀请码注册。",
  });
  if (!authenticated) {
    workflows.value = [];
    listError.value = "登录后可查看阶段工作流。";
    return;
  }
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
    const resolvedStage = routeStage ?? workflowCanvasStageFromCurrent(selectedWorkflow.value);
    activeCreateStage.value = resolvedStage === "final" ? "video" : resolvedStage;
    activeCanvasStage.value = resolvedStage;
    if (resolvedStage !== "final" && routeStage !== resolvedStage) {
      await router.replace({
        path: route.path,
        query: {
          ...route.query,
          stage: resolvedStage,
        },
      });
    }
    applyWorkflowDrafts(selectedWorkflow.value);
    previewStoryboardVersionId.value =
      selectedWorkflow.value.storyboardVersions.find((version) => version.selected)?.id
      ?? selectedWorkflow.value.storyboardVersions[0]?.id
      ?? "";
    if (selectedWorkflow.value.clipSlots.length) {
      const currentClipExists = selectedWorkflow.value.clipSlots.some((slot) => slot.clipIndex === selectedCanvasClipIndex.value);
      if (!currentClipExists) {
        selectedCanvasClipIndex.value = selectedWorkflow.value.clipSlots[0].clipIndex;
      }
    } else {
      selectedCanvasClipIndex.value = null;
    }
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
    return;
  }
  const authenticated = await requireAuth({
    title: "登录后创建画布",
    message: "阶段工作流会保存到你的账号下，请先登录或使用邀请码注册。",
  });
  if (!authenticated) {
    createError.value = "登录后可继续创建画布。";
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
    await loadWorkflows();
    openWorkflow(workflow.id, workflow.currentStage);
  } catch (error) {
    createError.value = error instanceof Error ? error.message : "创建工作流失败";
  } finally {
    creatingWorkflow.value = false;
  }
}

function resetTransientStageState() {
  closeCharacterAssetPicker();
  workflowSettingsOpen.value = false;
}

async function runAndRefresh(actionKey: string, runner: () => Promise<WorkflowDetail>) {
  const authenticated = await requireAuth({
    title: "登录后操作工作流",
    message: "工作流操作会修改你的个人数据，请先登录或使用邀请码注册。",
  });
  if (!authenticated) {
    detailError.value = "登录后可继续操作工作流。";
    return false;
  }
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

async function handleGenerateMissingCharacterSheets() {
  if (!selectedWorkflowId.value) {
    return;
  }
  const pendingClipIndexes = missingCharacterSheets.value
    .map((sheet) => characterSheetClipIndex(sheet))
    .filter((clipIndex): clipIndex is number => clipIndex !== null);
  if (!pendingClipIndexes.length) {
    return;
  }
  busyActionKey.value = "character-missing";
  detailError.value = "";
  try {
    for (const clipIndex of pendingClipIndexes) {
      selectedWorkflow.value = await generateKeyframe(selectedWorkflowId.value, clipIndex);
      applyWorkflowDrafts(selectedWorkflow.value);
    }
    await loadWorkflows();
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : "角色三视图生成失败";
  } finally {
    busyActionKey.value = "";
  }
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
  const authenticated = await requireAuth({
    title: "登录后删除工作流",
    message: "删除工作流会修改你的个人数据，请先登录或使用邀请码注册。",
  });
  if (!authenticated) {
    listError.value = "登录后可继续删除工作流。";
    return;
  }
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
  const authenticated = await requireAuth({
    title: "登录后删除版本",
    message: "删除版本会修改你的工作流数据，请先登录或使用邀请码注册。",
  });
  if (!authenticated) {
    detailError.value = "登录后可继续删除版本。";
    return;
  }
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
  const authenticated = await requireAuth({
    title: "登录后复用素材",
    message: "复用素材会创建你的阶段工作流，请先登录或使用邀请码注册。",
  });
  if (!authenticated) {
    detailError.value = "登录后可继续复用素材。";
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
      activeCanvasStage.value = resolvedStage;
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
.workflow-canvas-view {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  height: 100%;
  min-height: 0;
  padding: 22px;
  color: var(--text-strong);
  background: var(--bg-base);
}

.workflow-project-drawer,
.workflow-canvas-main,
.workflow-inspector,
.workflow-stage-board,
.workflow-composer-card,
.workflow-create-inspector,
.workflow-canvas-header,
.workflow-stage-pipeline,
.workflow-project-card,
.character-mini-card,
.clip-detail-card,
.storyboard-preview-card,
.version-side-panel,
.final-result-card-v2,
.inspector-section {
  border: 1px solid rgba(15, 20, 25, 0.07);
  background: #fff;
  box-shadow: var(--shadow-soft);
}

.workflow-project-drawer {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  padding: 18px;
  border-radius: 24px;
  overflow: hidden;
}

.workflow-drawer__head,
.workflow-canvas-header,
.stage-board__head,
.inspector-section__head,
.character-strip__head,
.version-side-panel__head,
.clip-detail-card__head,
.workflow-create-title,
.workflow-project-card__title,
.video-version-card__head,
.final-result-card-v2__meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.workflow-drawer__head h1,
.workflow-canvas-header h2,
.stage-board__head h3,
.inspector-section__head h3,
.workflow-create-title h2,
.storyboard-preview-card h4,
.clip-detail-card h4,
.final-result-card-v2 h4 {
  margin: 0;
  color: var(--text-strong);
  letter-spacing: -0.02em;
}

.workflow-eyebrow {
  margin: 0 0 6px;
  color: var(--text-muted);
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.drawer-icon-button,
.workflow-composer-submit {
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
}

.drawer-icon-button {
  width: 34px;
  height: 34px;
  background: #f3f6f8;
  color: var(--text-body);
}

.workflow-new-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  border: 0;
  border-radius: 16px;
  background: var(--bg-accent);
  color: #fff;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 12px 28px rgba(0, 161, 194, 0.2);
}

.workflow-new-button span {
  font-size: 1.25rem;
  line-height: 1;
}

.workflow-search-box,
.workflow-field {
  display: grid;
  gap: 8px;
}

.workflow-search-box span,
.workflow-field span {
  color: var(--text-body);
  font-size: 0.82rem;
  font-weight: 700;
}

.workflow-project-list {
  display: grid;
  gap: 10px;
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
}

.workflow-project-card {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 30px;
  align-items: start;
  border-radius: 18px;
  overflow: visible;
}

.workflow-project-card-active {
  border-color: rgba(0, 161, 194, 0.28);
  box-shadow: var(--shadow-glow);
}

.workflow-project-card__open {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.workflow-project-card__title strong,
.compact-version-card strong,
.character-mini-card strong,
.video-version-card strong,
.missing-clip-list strong {
  color: var(--text-strong);
  line-height: 1.35;
}

.workflow-project-card__title span,
.workflow-project-card__stats,
.workflow-project-card__meta,
.compact-version-card span,
.character-mini-card p,
.clip-detail-card p,
.final-result-card-v2 span,
.inspector-kv-list span,
.workflow-finalize-box__hint {
  color: var(--text-muted);
  font-size: 0.78rem;
  line-height: 1.55;
}

.workflow-project-card__stats,
.workflow-project-card__meta,
.workflow-summary__meta,
.readiness-strip,
.character-mini-card__actions,
.version-card__actions,
.rating-row,
.workflow-inspector-actions,
.character-asset-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.workflow-more-menu {
  position: relative;
  padding: 8px 8px 0 0;
}

.workflow-more-menu summary {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  color: var(--text-muted);
  cursor: pointer;
  list-style: none;
}

.workflow-more-menu summary::-webkit-details-marker {
  display: none;
}

.workflow-more-menu[open] summary,
.workflow-more-menu summary:hover {
  background: #eef2f4;
  color: var(--text-strong);
}

.workflow-more-menu[open]::after {
  content: "";
  position: fixed;
  inset: 0;
  z-index: 10;
}

.workflow-more-menu > button,
.workflow-more-menu > a {
  position: absolute;
  top: 38px;
  right: 8px;
  z-index: 11;
  display: flex;
  width: 150px;
  min-height: 34px;
  align-items: center;
  padding: 0 12px;
  border: 0;
  border-bottom: 1px solid rgba(15, 20, 25, 0.06);
  background: #fff;
  color: var(--text-strong);
  font-size: 0.8rem;
  text-align: left;
  cursor: pointer;
}

.workflow-more-menu > button:nth-child(3),
.workflow-more-menu > a:nth-child(3) {
  top: 72px;
}

.workflow-more-menu > button:nth-child(4),
.workflow-more-menu > a:nth-child(4) {
  top: 106px;
}

.workflow-more-menu > button:nth-child(5),
.workflow-more-menu > a:nth-child(5) {
  top: 140px;
}

.workflow-more-menu > button:nth-child(2),
.workflow-more-menu > a:nth-child(2) {
  border-radius: 12px 12px 0 0;
}

.workflow-more-menu > button:last-child,
.workflow-more-menu > a:last-child {
  border-bottom: 0;
  border-radius: 0 0 12px 12px;
}

.workflow-more-menu[open] > button,
.workflow-more-menu[open] > a {
  box-shadow: var(--shadow-panel);
}

.workflow-more-menu:not([open]) > button,
.workflow-more-menu:not([open]) > a {
  display: none;
}

.workflow-menu-danger {
  color: var(--accent-danger) !important;
}

.workflow-canvas-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  min-height: 0;
  padding: 18px;
  border-radius: 28px;
  overflow: auto;
}

.workflow-create-board,
.workflow-canvas-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.workflow-create-composer {
  display: grid;
  gap: 28px;
  justify-items: center;
  padding: 38px 18px;
}

.workflow-create-title {
  width: min(100%, 980px);
}

.workflow-composer-card {
  position: relative;
  display: grid;
  gap: 16px;
  width: min(100%, 980px);
  min-height: 340px;
  padding: 24px 62px 18px 24px;
  border-radius: 26px;
}

.workflow-composer-title-field,
.workflow-composer-body-field,
.workflow-composer-prompt-field {
  display: grid;
  gap: 8px;
}

.workflow-composer-title-field span,
.workflow-composer-body-field span,
.workflow-composer-prompt-field span {
  color: var(--text-muted);
  font-size: 0.74rem;
  font-weight: 800;
}

.workflow-composer-title-field input,
.workflow-composer-body-field textarea,
.workflow-composer-prompt-field textarea {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-strong);
}

.workflow-composer-title-field input {
  font-size: 1.08rem;
  font-weight: 800;
}

.workflow-composer-body-field textarea,
.workflow-composer-prompt-field textarea {
  line-height: 1.7;
}

.workflow-composer-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding-top: 4px;
}

.tool-pill {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 13px;
  border: 1px solid rgba(15, 20, 25, 0.06);
  border-radius: 9px;
  background: #fff;
  color: var(--text-strong);
  font-size: 0.78rem;
  font-weight: 700;
}

.tool-pill-accent {
  color: var(--accent-cyan);
}

.composer-required-count {
  margin-left: auto;
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 800;
}

.workflow-composer-submit {
  position: absolute;
  right: 18px;
  bottom: 18px;
  width: 40px;
  height: 40px;
  background: var(--accent-cyan);
  color: #fff;
  font-size: 1.2rem;
  box-shadow: 0 12px 28px rgba(0, 161, 194, 0.24);
}

.workflow-composer-submit:disabled,
.workflow-new-button:disabled,
button:disabled {
  cursor: not-allowed;
  opacity: 0.54;
  box-shadow: none;
}

.workflow-canvas-header,
.workflow-stage-pipeline,
.workflow-stage-board,
.workflow-inspector,
.workflow-create-inspector {
  border-radius: 24px;
  padding: 18px;
}

.workflow-stage-pipeline {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.workflow-stage-step {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 64px;
  padding: 10px;
  border: 1px solid rgba(15, 20, 25, 0.07);
  border-radius: 18px;
  background: #f8fafb;
  color: var(--text-body);
  text-align: left;
  cursor: pointer;
}

.workflow-stage-step-active,
.workflow-stage-step:hover {
  border-color: rgba(0, 161, 194, 0.28);
  background: rgba(0, 161, 194, 0.08);
  color: var(--accent-cyan);
}

.workflow-stage-step__index {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  color: currentColor;
  font-weight: 900;
}

.workflow-stage-step__text {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.workflow-stage-step__text strong {
  color: var(--text-strong);
}

.workflow-stage-step__text small,
.workflow-stage-step__count {
  color: var(--text-muted);
  font-size: 0.72rem;
}

.workflow-stage-step-ready .workflow-stage-step__count {
  color: var(--accent-cyan);
}

.workflow-stage-canvas {
  min-width: 0;
}

.workflow-stage-board {
  display: grid;
  gap: 16px;
  min-height: 620px;
}

.storyboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 14px;
  min-height: 0;
}

.storyboard-preview-card,
.version-side-panel,
.clip-detail-card,
.character-strip,
.missing-clip-list {
  display: grid;
  gap: 14px;
  padding: 16px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid rgba(15, 20, 25, 0.07);
}

.storyboard-preview-card__head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.storyboard-preview-markdown,
.version-card__markdown {
  min-height: 420px;
  max-height: 62vh;
  overflow: auto;
  padding: 18px;
  border-radius: 18px;
  background: #f8fafb;
  color: var(--text-body);
  line-height: 1.75;
}

.storyboard-preview-markdown :deep(h1),
.storyboard-preview-markdown :deep(h2),
.storyboard-preview-markdown :deep(h3),
.storyboard-preview-markdown :deep(h4) {
  color: var(--text-strong);
}

.storyboard-preview-markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.storyboard-preview-markdown :deep(th),
.storyboard-preview-markdown :deep(td) {
  padding: 8px 9px;
  border: 1px solid rgba(15, 20, 25, 0.08);
  vertical-align: top;
}

.compact-version-list {
  display: grid;
  gap: 8px;
  max-height: 460px;
  overflow: auto;
  padding-right: 2px;
}

.horizontal-version-list,
.video-version-grid,
.character-strip__list,
.character-asset-grid,
.keyframe-thumb-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
  max-height: none;
  overflow: visible;
}

.compact-version-card,
.video-version-card,
.keyframe-frame-card,
.keyframe-thumb-card,
.character-asset-card,
.missing-clip-card {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid rgba(15, 20, 25, 0.07);
  border-radius: 16px;
  background: #f8fafb;
}

.compact-version-card-active,
.video-version-card-active {
  border-color: rgba(0, 161, 194, 0.28);
  background: rgba(0, 161, 194, 0.08);
}

.compact-version-card__main,
.missing-clip-card {
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.storyboard-adjust-panel {
  display: grid;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(15, 20, 25, 0.07);
}

.character-strip {
  background: #f8fafb;
}

.character-mini-card {
  padding: 14px;
  border-radius: 18px;
}

.character-mini-card p,
.clip-detail-card p {
  margin: 6px 0 0;
}

.character-mini-card__frames {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.character-mini-frame {
  position: relative;
  display: block;
  min-height: 96px;
  padding: 0;
  border: 0;
  border-radius: 14px;
  overflow: hidden;
  background: #eef2f4;
  cursor: zoom-in;
}

.character-mini-frame img {
  width: 100%;
  height: 100%;
  min-height: 96px;
  object-fit: cover;
}

.character-mini-frame span {
  position: absolute;
  left: 6px;
  bottom: 6px;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-strong);
  font-size: 0.7rem;
  font-weight: 800;
  line-height: 22px;
}

.character-asset-picker {
  grid-column: 1 / -1;
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(15, 20, 25, 0.07);
  border-radius: 18px;
  background: #fff;
}

.character-asset-picker__head,
.character-asset-picker__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  justify-content: space-between;
}

.character-asset-card__preview {
  display: block;
  width: 100%;
  height: 148px;
  padding: 0;
  border: 0;
  border-radius: 14px;
  overflow: hidden;
  background: #eef2f4;
  cursor: zoom-in;
}

.character-asset-card__preview img,
.keyframe-frame-card__image,
.keyframe-thumb-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.clip-workbench {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 14px;
}

.clip-timeline {
  display: grid;
  align-content: start;
  gap: 8px;
  max-height: 620px;
  overflow: auto;
}

.clip-timeline__item {
  display: grid;
  gap: 4px;
  min-height: 60px;
  padding: 12px;
  border: 1px solid rgba(15, 20, 25, 0.07);
  border-radius: 16px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.clip-timeline__item-active,
.clip-timeline__item:hover {
  border-color: rgba(0, 161, 194, 0.28);
  background: rgba(0, 161, 194, 0.08);
}

.clip-timeline__item span {
  color: var(--text-muted);
  font-size: 0.76rem;
}

.keyframe-frame-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.character-sheet-preview-trigger {
  display: block;
  width: 100%;
  height: 260px;
  padding: 0;
  border: 0;
  border-radius: 16px;
  overflow: hidden;
  background: #eef2f4;
  cursor: zoom-in;
}

.keyframe-frame-card__head,
.keyframe-frame-card__actions,
.clip-card__selected-keyframes-head {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.keyframe-frame-card__failure {
  display: grid;
  place-items: center;
  min-height: 180px;
  padding: 16px;
  border-radius: 14px;
  background: #fff4f6;
  color: var(--accent-danger);
  text-align: center;
}

.readiness-strip {
  padding: 12px 14px;
  border-radius: 16px;
  background: #f3f6f8;
  color: var(--text-body);
  font-weight: 700;
}

.video-version-card .version-card__video,
.final-result-card__video {
  width: 100%;
  max-height: 380px;
  border-radius: 16px;
  background: #eef2f4;
  object-fit: cover;
}

.final-result-card-v2 {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 16px;
  padding: 16px;
  border-radius: 20px;
}

.final-result-card-v2__meta {
  flex-direction: column;
}

.workflow-kv,
.inspector-kv-list,
.workflow-settings-stack {
  display: grid;
  gap: 10px;
}

.workflow-kv__row,
.inspector-kv-list div {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(15, 20, 25, 0.06);
}

.workflow-kv__row strong,
.inspector-kv-list strong {
  color: var(--text-strong);
  text-align: right;
}

.workflow-inspector {
  position: sticky;
  top: 0;
  display: grid;
  gap: 14px;
  max-height: calc(100vh - 150px);
  overflow: auto;
}

.inspector-section {
  display: grid;
  gap: 14px;
  padding: 16px;
  border-radius: 20px;
}

.inspector-section__head {
  display: block;
}

.stage-toggle-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stage-toggle-chip,
.rating-pill {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(15, 20, 25, 0.08);
  border-radius: 999px;
  background: #fff;
  color: var(--text-body);
  font-size: 0.82rem;
  font-weight: 800;
  cursor: pointer;
}

.stage-toggle-chip-active,
.rating-pill-active {
  border-color: rgba(0, 161, 194, 0.24);
  background: rgba(0, 161, 194, 0.08);
  color: var(--accent-cyan);
}

.workflow-empty,
.workflow-error {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.65;
}

.workflow-error {
  color: var(--accent-danger);
}

.workflow-empty-large {
  display: grid;
  place-items: center;
  min-height: 240px;
  padding: 28px;
  text-align: center;
}

.workflow-empty-soft,
.workflow-empty-nested {
  padding: 18px;
  border-radius: 16px;
  background: #f8fafb;
}

.workflow-banner {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border-radius: 18px;
}

.workflow-banner-error {
  border-color: rgba(229, 72, 101, 0.2);
  background: #fff4f6;
}

.image-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 28px;
  background: rgba(15, 20, 25, 0.74);
  backdrop-filter: blur(12px);
}

.image-preview-full {
  display: block;
  max-width: min(92vw, 1280px);
  max-height: 82vh;
  object-fit: contain;
  border-radius: 18px;
  background: #eef2f4;
}

.image-preview-caption,
.image-preview-close {
  position: fixed;
  z-index: 1;
  color: #fff;
}

.image-preview-caption {
  left: 28px;
  top: 24px;
  display: grid;
  gap: 4px;
}

.image-preview-caption span {
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.82rem;
}

.image-preview-close {
  right: 28px;
  top: 24px;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  cursor: pointer;
}

@media (max-width: 1380px) {
  .workflow-canvas-view {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .workflow-create-board,
  .workflow-canvas-grid,
  .storyboard-layout,
  .final-result-card-v2 {
    grid-template-columns: 1fr;
  }

  .workflow-inspector {
    position: static;
    max-height: none;
  }
}

@media (max-width: 1024px) {
  .workflow-canvas-view {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 100%;
    overflow: auto;
  }

  .workflow-canvas-view-detail .workflow-canvas-main {
    order: 1;
  }

  .workflow-canvas-view-detail .workflow-project-drawer {
    order: 2;
  }

  .workflow-project-drawer {
    max-height: none;
  }

  .workflow-stage-pipeline {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .clip-workbench {
    grid-template-columns: 1fr;
  }

  .clip-timeline {
    grid-auto-flow: column;
    grid-auto-columns: minmax(180px, 1fr);
    grid-template-columns: none;
    overflow-x: auto;
    overflow-y: hidden;
  }
}

@media (max-width: 640px) {
  .workflow-canvas-view {
    padding: 14px;
  }

  .workflow-stage-pipeline {
    grid-template-columns: 1fr;
  }

  .workflow-composer-card {
    padding: 18px;
  }

  .workflow-composer-submit {
    position: static;
    margin-left: auto;
  }

  .composer-required-count {
    width: 100%;
    margin-left: 0;
  }

  .workflow-canvas-header,
  .stage-board__head,
  .workflow-drawer__head,
  .storyboard-preview-card__head,
  .clip-detail-card__head,
  .keyframe-frame-card__head,
  .keyframe-frame-card__actions,
  .workflow-banner {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
