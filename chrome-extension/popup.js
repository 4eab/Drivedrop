function updateUI(isLoggedIn) {
    document.getElementById("authSection").style.display = isLoggedIn ? "none" : "block";
    document.getElementById("uploadSection").style.display = isLoggedIn ? "block" : "none";
}

document.addEventListener("DOMContentLoaded", async () => {
    chrome.storage.local.get(["token"], async (result) => {
        if (result.token) {
            try {
                const res = await fetch("http://localhost:8080/api/auth/me", {
                    headers: { Authorization: "Bearer " + result.token }
                });
                if (res.ok) {
                    updateUI(true);
                    return;
                }
            } catch (e) { console.error(e); }
        }
        updateUI(false);
    });

    document.getElementById("logoutBtn").addEventListener("click", () => {
        chrome.storage.local.remove("token");
        setTimeout(() => updateUI(false), 100);
    });

    document.getElementById("loginBtn").addEventListener("click", () => {
        window.open(
            "http://localhost:8080/api/auth/login",
            "login_window",
            "width=450,height=600,top=150,left=450"
        );
    });

    document.getElementById("uploadBtn").addEventListener("click", () => {
        chrome.tabs.create({ url: 'upload.html' });
    });
});