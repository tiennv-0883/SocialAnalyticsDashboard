(function () {
  const root = document.getElementById("charts");
  if (!root) {
    return;
  }
  const css = getComputedStyle(root);
  const color = (name) => css.getPropertyValue(name).trim();
  const numberFormat = new Intl.NumberFormat("vi-VN");
  const platformFilter = document.getElementById("platformFilter");
  const daysFilter = document.getElementById("daysFilter");

  const axisOptions = {
    x: {
      grid: { display: false },
      ticks: { color: "#656d76", maxRotation: 0, autoSkip: true },
    },
    y: {
      beginAtZero: true,
      grid: { color: color("--grid") },
      border: { display: false },
      ticks: {
        color: "#656d76",
        callback: (value) => numberFormat.format(value),
      },
    },
  };
  const lineDataset = (label, slot) => ({
    label,
    data: [],
    borderColor: color(slot),
    backgroundColor: color(slot),
    borderWidth: 2,
    pointRadius: 0,
    pointHoverRadius: 4,
    pointHitRadius: 12,
    tension: 0.2,
  });
  const tooltip = {
    mode: "index",
    intersect: false,
    callbacks: {
      label: (ctx) =>
        " " + ctx.dataset.label + ": " + numberFormat.format(ctx.parsed.y),
    },
  };

  const engagementChart = new Chart(
    document.getElementById("engagementChart"),
    {
      type: "line",
      data: {
        labels: [],
        datasets: [
          lineDataset("Likes", "--series-1"),
          lineDataset("Shares", "--series-2"),
          lineDataset("Comments", "--series-3"),
        ],
      },
      options: {
        maintainAspectRatio: false,
        animation: false,
        interaction: { mode: "index", intersect: false },
        plugins: {
          legend: {
            position: "top",
            align: "start",
            labels: { usePointStyle: true, pointStyle: "line" },
          },
          tooltip,
        },
        scales: axisOptions,
      },
    },
  );
  const followersChart = new Chart(document.getElementById("followersChart"), {
    type: "line",
    data: { labels: [], datasets: [lineDataset("Followers", "--series-1")] },
    options: {
      maintainAspectRatio: false,
      animation: false,
      interaction: { mode: "index", intersect: false },
      plugins: { legend: { display: false }, tooltip },
      scales: axisOptions,
    },
  });

  let loading = null;
  async function loadChart() {
    if (loading) {
      return loading;
    }
    const params = new URLSearchParams({ days: daysFilter.value });
    if (platformFilter.value) {
      params.set("platform", platformFilter.value);
    }
    loading = fetch("/chart-data?" + params, {
      credentials: "same-origin",
      headers: { Accept: "application/json" },
    })
      .then((response) => {
        if (response.status === 401) {
          window.location.href = "/login";
          throw new Error("unauthorized");
        }
        if (!response.ok) {
          throw new Error("HTTP " + response.status);
        }
        return response.json();
      })
      .then(render)
      .catch((error) => console.warn("Cannot load chart data", error))
      .finally(() => {
        loading = null;
      });
    return loading;
  }

  function render(data) {
    const labels = data.labels.map((day) =>
      day.slice(5).split("-").reverse().join("/"),
    );
    engagementChart.data.labels = labels;
    engagementChart.data.datasets[0].data = data.series.likes;
    engagementChart.data.datasets[1].data = data.series.shares;
    engagementChart.data.datasets[2].data = data.series.comments;
    engagementChart.update();
    followersChart.data.labels = labels;
    followersChart.data.datasets[0].data = data.series.followers;
    followersChart.update();

    const rows = data.labels.map(
      (day, i) =>
        "<tr><td>" +
        day +
        "</td>" +
        ["likes", "shares", "comments", "followers"]
          .map(
            (name) =>
              "<td>" + numberFormat.format(data.series[name][i]) + "</td>",
          )
          .join("") +
        "</tr>",
    );
    document.querySelector("#chartTable tbody").innerHTML = rows.join("");
    document.getElementById("chartUpdated").textContent =
      "Cập nhật lúc " + new Date(data.generatedAt).toLocaleTimeString("vi-VN");
  }

  function showToast(message) {
    const toast = document.createElement("div");
    toast.className = "toast";
    toast.textContent = message;
    document.getElementById("toasts").appendChild(toast);
    setTimeout(() => toast.remove(), 6000);
  }

  function setLive(connected) {
    const status = document.getElementById("liveStatus");
    status.classList.toggle("on", connected);
    status.lastElementChild.textContent = connected
      ? "Realtime: đã kết nối"
      : "Realtime: mất kết nối, đang thử lại...";
  }

  const RELOAD_DELAY_MS = 1500;
  const RELOAD_JITTER_MS = 3000;
  let reloadTimer = null;
  let reloadWhenVisible = false;

  function scheduleReload() {
    if (document.hidden) {
      reloadWhenVisible = true;
      return;
    }
    clearTimeout(reloadTimer);
    reloadTimer = setTimeout(
      loadChart,
      RELOAD_DELAY_MS + Math.random() * RELOAD_JITTER_MS,
    );
  }

  document.addEventListener("visibilitychange", () => {
    if (!document.hidden && reloadWhenVisible) {
      reloadWhenVisible = false;
      scheduleReload();
    }
  });

  const stomp = new StompJs.Client({
    webSocketFactory: () => new SockJS("/ws"),
    reconnectDelay: 5000,
    debug: () => {},
  });
  stomp.onConnect = () => {
    setLive(true);
    stomp.subscribe("/topic/metrics", (frame) => {
      const notification = JSON.parse(frame.body);
      showToast(notification.message);
      scheduleReload();
    });
  };
  stomp.onWebSocketClose = () => setLive(false);
  stomp.onStompError = (frame) =>
    console.warn("STOMP error", frame.headers.message);
  stomp.activate();

  platformFilter.addEventListener("change", loadChart);
  daysFilter.addEventListener("change", loadChart);
  loadChart();
})();
