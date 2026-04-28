function updateUI(state) {
  const authSection = document.getElementById("authSection");
  const uploadSection = document.getElementById("uploadSection");
  const shareSection = document.getElementById("shareSection");
  const filePickerArea = document.getElementById("filePickerArea");

  authSection.style.display = state === 'auth' ? "block" : "none";
  uploadSection.style.display = (state === 'upload' || state === 'share') ? "block" : "none";
  shareSection.style.display = state === 'share' ? "block" : "none";
  filePickerArea.style.display = state === 'share' ? "none" : "block";
}

function setStatus(msg) {
  document.getElementById("status").innerText = msg;
}

let currentFileId = null;
let currentTaskId = null;
let currentFileName = null;

document.addEventListener("DOMContentLoaded", async () => {
  const tokenObj = await chrome.storage.local.get(["token"]);
  const token = tokenObj.token;

  if (!token) {
    updateUI('auth');
  } else {
    checkToken(token);
  }

  // 1. login
  document.getElementById("loginBtn").addEventListener("click", () => {
    window.open("http://localhost:8080/api/auth/login", "login", "width=450,height=600");
  });

  // 2. logout
  document.getElementById("logoutBtn").addEventListener("click", () => {
    chrome.storage.local.remove("token", () => location.reload());
  });

  // 3. upload
  document.getElementById("uploadBtn").addEventListener("click", async () => {
    const fileInput = document.getElementById("fileInput");
    if (fileInput.files.length === 0) return setStatus("Please select a file.");

    const file = fileInput.files[0];
    currentFileName = file.name;
    setStatus("Getting upload URL...");


    try {
      const params = new URLSearchParams();
      params.append('fileName', currentFileName);
      const initRes = await fetch(`http://localhost:8080/api/tasks/init?${params.toString()}`, {
        method: 'POST',
        headers: {
          "Authorization": `Bearer ${token}`
        }
      });

      if (!initRes.ok) throw new Error("Please Sign In First");

      const { taskId, uploadUrl } = await initRes.json();
      currentTaskId = taskId;

      setStatus("Upload to Google Drive (0%)...");

      const xhr = new XMLHttpRequest();
      xhr.open("PUT", uploadUrl, true);

      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          const percent = Math.round((e.loaded / e.total) * 100);
          setStatus(`Upload to Google Drive (${percent}%)...`);
        }
      };

      xhr.onload = () => {
        if (xhr.status === 200 || xhr.status === 201) {
          try {
            const response = JSON.parse(xhr.responseText);
            const googleFileId = response.id;
            console.log("Google File ID:", googleFileId);

            currentFileId = googleFileId

            setStatus("Upload completed");
            updateUI('share');
          } catch (e) {
            console.error("Failed to parse Google Response", e);
          }
        } else {
          setStatus("Upload failed: " + xhr.status);
        }
      };

      xhr.onerror = () => setStatus("Something went wrong...");
      xhr.send(file);
      currentFileId = xhr.get

    } catch (err) {
      setStatus("ERROR: " + err.message);
    }
  });

  document.getElementById("confirmShareBtn").addEventListener("click", async () => {
    const mode = document.getElementById("shareMode").value;
    const limit = document.getElementById("shareLimit").value;
    if (isNaN(limit) || limit < 1) {
      setStatus("Invalid Input!");
      return;
    }

    setStatus("Generating link...");

    try {
      const res = await fetch(`http://localhost:8080/api/tasks/confirm`, {
        method: 'POST',
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          taskId: currentTaskId,
          googleFileId: currentFileId,
          fileName: currentFileName,
          shareMode: mode,
          maxDownloads: limit
        })
      });

      const data = await res.json();

      let contentHtml = '';

      // TODO: COPY DOES NOT WORK
      if (data.authMode === 'DISTRIBUTED') {
        contentHtml = `
            <p style="color: green;"><b>Distributed Mode:</b> Created ${data.totalCodes} links</p>
            <div style="max-height: 200px; overflow-y: auto; border: 1px solid #ddd; padding: 5px;">
                ${data.accessLinks.map(link => `
                    <div style="display: flex; margin-bottom: 5px; gap: 5px;">
                        <input type="text" value="${link.downloadUrl}" readonly style="flex:1; font-size:12px;">
                        <button onclick="navigator.clipboard.writeText('${link.downloadUrl}')">Copy</button>
                    </div>
                `).join('')}
            </div>
            <p style="font-size: 11px; color: #666; margin-top: 5px;">* Each link is one-time use (burn after download).</p>
        `;
      } else {
        contentHtml = `
            <p style="color: green;"><b>Public Mode:</b> Ready!</p>
            <div style="display: flex; gap: 5px;">
                <input type="text" id="publicUrl" value="${data.publicUrl}" readonly style="flex:1; padding:5px;">
                <button onclick="navigator.clipboard.writeText(document.getElementById('publicUrl').value)">Copy</button>
            </div>
        `;
      }

      const shareSection = document.getElementById("shareSection");
      shareSection.innerHTML = contentHtml;

      setStatus("Task Initialized!");

    } catch (err) {
      console.error(err);
      setStatus("Initialization failed.");
    }
  });
});

async function checkToken(token) {
  try {
    const res = await fetch("http://localhost:8080/api/auth/me", {
      headers: { "Authorization": "Bearer " + token }
    });
    updateUI(res.ok ? 'upload' : 'auth');
  } catch (e) {
    updateUI('auth');
  }
}