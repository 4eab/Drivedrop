chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
 if (changeInfo.url) {
    
    if (changeInfo.url.includes("http://localhost:8080/callback.html/?token=")) {
    const url = new URL(changeInfo.url);
    const token = url.searchParams.get("token");

    if (token) {
      chrome.storage.local.set({ "token": token }, () => {
        console.log("Service Worker has received and stored the Token");
        
        chrome.tabs.remove(tabId);
        chrome.tabs.create({ url: 'upload.html' });
      });
    }
  }}
});