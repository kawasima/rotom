// Helpers
function pageName() {
  return typeof pageFullPath === "undefined" ? undefined : pageFullPath.split("/").pop();
}
function pagePath() {
  return typeof pageFullPath === "undefined" ? undefined : pageFullPath.split("/").slice(0, -1).join("/");
}

function htmlEscape(str) {
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

document.addEventListener("DOMContentLoaded", function () {
  // Delete link confirmation
  var deleteLink = document.getElementById("delete-link");
  if (deleteLink) {
    deleteLink.addEventListener("click", function (e) {
      e.preventDefault();
      if (confirm(this.dataset.confirm)) {
        this.closest("form").submit();
      }
    });
  }

  // History page: version comparison checkboxes
  var wikiHistory = document.getElementById("wiki-history");
  if (wikiHistory) {
    var node1 = null;
    var node2 = null;

    function clearSelections() {
      wikiHistory.querySelectorAll("tr.selected").forEach(function (tr) {
        tr.classList.remove("selected");
      });
    }

    function selectRange(n1, n2) {
      if (!n1 || !n2) return;
      clearSelections();
      var rows = Array.from(wikiHistory.querySelectorAll("tbody tr"));
      var i1 = rows.indexOf(n1);
      var i2 = rows.indexOf(n2);
      if (i1 > i2) { var tmp = i1; i1 = i2; i2 = tmp; }
      for (var i = i1; i <= i2; i++) {
        rows[i].classList.add("selected");
      }
    }

    wikiHistory.querySelectorAll("td.checkbox input").forEach(function (checkbox) {
      checkbox.addEventListener("click", function () {
        var row = this.closest("tr");
        if (!this.checked) {
          clearSelections();
          if (row === node1) {
            node1 = node2;
            node2 = null;
          } else if (row === node2) {
            node2 = null;
          }
          if (node1) node1.classList.add("selected");
          if (node2) { node2.classList.add("selected"); selectRange(node1, node2); }
        } else {
          if (!node1) {
            node1 = row;
            node1.classList.add("selected");
          } else if (!node2) {
            node2 = row;
            selectRange(node1, node2);
          } else {
            this.checked = false;
          }
        }
      });

      if (checkbox.checked) {
        checkbox.click();
        checkbox.click();
      }
    });

    var compareBtn = document.querySelector(".history a.action-compare-revision");
    if (compareBtn) {
      compareBtn.addEventListener("click", function (e) {
        e.preventDefault();
        var form = document.getElementById("version-form");
        if (form) form.submit();
      });
    }
  }

  // Search bar
  var searchSubmit = document.querySelector("#searchbar a#search-submit");
  if (searchSubmit) {
    searchSubmit.addEventListener("click", function (e) {
      e.preventDefault();
      var form = document.querySelector("#searchbar #search-form");
      if (form) form.submit();
    });
  }

  // Revert button
  var revertBtn = document.querySelector("a.gollum-revert-button");
  if (revertBtn) {
    revertBtn.addEventListener("click", function (e) {
      e.preventDefault();
      var form = document.getElementById("gollum-revert-form");
      if (form) form.submit();
    });
  }
});
