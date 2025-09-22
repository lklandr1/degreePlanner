// settings.js
import { getAuth, onAuthStateChanged, updateEmail, updatePassword } from "https://www.gstatic.com/firebasejs/12.3.0/firebase-auth.js";
import { getDatabase, ref, get, update } from "https://www.gstatic.com/firebasejs/12.3.0/firebase-database.js";

export function loadSettings(app, contentId = "content") {
    const auth = getAuth(app);
    const database = getDatabase(app);
    const _ = (element) => document.getElementById(element);
    onAuthStateChanged(auth, (user) => {
        console.log("onAuthStateChanged fired", user);
        if (user) {
            get(ref(database, 'users/' + user.uid)).then((snapshot) => {
                console.log("Firebase get returned", snapshot.exists(), snapshot.val());
                if (snapshot.exists()) {
                    const userInfo = snapshot.val();
                    let settingsHtml = `<form id="settings_form">
                        <input type="text" name="username" value="${userInfo.username || ''}" placeholder="Username"><br>
                        <input type="email" name="email" value="${userInfo.email || ''}" placeholder="Email"><br>
                        <input type="password" name="password" placeholder="New Password"><br>
                        <input type="password" name="password2" placeholder="Confirm Password"><br>
                        <input type="button" value="Save" id="save_button">
                        <div id="settings_message" style="color: red; margin-top: 10px;"></div>
                    </form>`;
                    _(contentId).innerHTML = settingsHtml;
                    setupSettingsForm(user.uid, user.email, app, contentId);
                } else {
                    // Show a blank form if no user data exists
                    let settingsHtml = `<form id="settings_form">
                        <input type="text" name="username" value="" placeholder="Username"><br>
                        <input type="email" name="email" value="${user.email || ''}" placeholder="Email"><br>
                        <input type="password" name="password" placeholder="New Password"><br>
                        <input type="password" name="password2" placeholder="Confirm Password"><br>
                        <input type="button" value="Save" id="save_button">
                        <div id="settings_message" style="color: red; margin-top: 10px;"></div>
                    </form>`;
                    _(contentId).innerHTML = settingsHtml;
                    setupSettingsForm(user.uid, user.email, app, contentId);
                }
            }).catch(err => {
                console.error("Firebase get error", err);
                _(contentId).innerHTML = "<div>Error loading user info.</div>";
            });
        } else {
            window.location = "login.html";
        }
    });
}

function setupSettingsForm(uid, currentEmail, app, contentId) {
    var save_button = document.getElementById("save_button");
    if (save_button) {
        save_button.addEventListener("click", function() { collect_Data(uid, currentEmail, app, contentId); });
    }
}

function collect_Data(uid, currentEmail, app, contentId) {
    const auth = getAuth(app);
    const database = getDatabase(app);
    var save_button = document.getElementById("save_button");
    save_button.disabled = true;
    save_button.value = "Loading";

    var form = document.getElementById("settings_form");
    var username = form.username.value;
    var email = form.email.value;
    var password = form.password.value;
    var password2 = form.password2.value;
    var messageDiv = document.getElementById("settings_message");
    messageDiv.innerHTML = "";

    if (password !== password2) {
        messageDiv.innerHTML = "Passwords do not match.";
        save_button.disabled = false;
        save_button.value = "Save";
        return;
    }

    const user = auth.currentUser;
    let updates = { username: username, email: email };
    let promises = [update(ref(database, 'users/' + uid), updates)];
    if (user && email !== currentEmail) {
        promises.push(updateEmail(user, email));
    }
    if (user && password) {
        promises.push(updatePassword(user, password));
    }
    Promise.all(promises)
        .then(() => {
            messageDiv.style.color = "black";
            messageDiv.innerHTML = "Account updated successfully.";
        })
        .catch((error) => {
            messageDiv.style.color = "black";
            if (error.code === "auth/requires-recent-login") {
                reauthenticateAndRetry(user, uid, currentEmail, app, contentId, messageDiv, save_button);
            } else {
                messageDiv.innerHTML = "Failed to update account: " + error.message;
            }
        })
        .finally(() => {
            save_button.disabled = false;
            save_button.value = "Save";
        });
}

async function reauthenticateAndRetry(user, uid, currentEmail, app, contentId, messageDiv, save_button) {
    const password = prompt("For security, please re-enter your password to update your account:");
    if (password) {
        const { EmailAuthProvider, reauthenticateWithCredential } = await import("https://www.gstatic.com/firebasejs/12.3.0/firebase-auth.js");
        const credential = EmailAuthProvider.credential(user.email, password);
        reauthenticateWithCredential(user, credential)
            .then(() => {
                collect_Data(uid, currentEmail, app, contentId);
            })
            .catch((reauthError) => {
                messageDiv.innerHTML = "Re-authentication failed: " + reauthError.message;
                save_button.disabled = false;
                save_button.value = "Save";
            });
    } else {
        messageDiv.innerHTML = "Re-authentication cancelled.";
        save_button.disabled = false;
        save_button.value = "Save";
    }
}
