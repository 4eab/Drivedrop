
# Drivedrop

Securely drop files to your friends via Google Drive, then watch them drop out of existence when the share is done.


## The Lifecycle

1. **Upload**: Files are streamed to your default folder in your Google Drive via the Chrome Extension.
2. **Initialization**: A `ShareTask` is created using one of two modes:
    - **Public Mode (`OPEN_CLAIM`)**: Generates a single public URL. Anyone with the link can claim a download slot until the total quota is exhausted. Best for "first-come, first-served" group sharing.
    - **Distributed Mode (`DISTRIBUTED`)**: Generates $N$ unique, individual access codes. Each code is locked to a specific recipient.
3. **The Claim**: When a recipient clicks the link, the system locks the code to prevent race condition.
4. **The Stream**: The backend acts as a **transparent pipe**. It pulls the file from Google Drive and streams it directly to the user's browser. 
    - **Privacy Note**: The server **never** stores the file on its local disk and **never** accesses, reads, or logs the content of your files. Data exists only in volatile memory during the transfer.
5. **The Burn**: Once the last byte is successfully sent, the code is marked as `used`. If the system detects that no valid codes remain for the task, it triggers an immediate physical deletion of the file from Google Drive.
