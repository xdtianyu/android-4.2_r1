function onLoad() {
  reload();
}

function reload() {
  if (!linuxPerfData)
    return;

  var m = new tracing.TimelineModel();
  m.importEvents("[]", true, [linuxPerfData]);

  var timelineViewEl = document.querySelector('.view');
  cr.ui.decorate(timelineViewEl, tracing.TimelineView);
  timelineViewEl.model = m;
  timelineViewEl.tabIndex = 1;
  timelineViewEl.timeline.focusElement = timelineViewEl;
}

document.addEventListener('DOMContentLoaded', onLoad);
