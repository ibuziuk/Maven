var mailList = [];
function run() {
    startServer();
    testServer();
}
function mouseOut() {
    for (var j = 0; j < this.childNodes.length; j++) {
        if (this.childNodes[j].className == "rec") {
            this.childNodes[j].style.visibility = "hidden";
        }
        if (this.childNodes[j].className == "del") {
            this.childNodes[j].style.visibility = "hidden";
        }
    }
}
function mouseOver() {
    for (var j = 0; j < this.childNodes.length; j++) {
        if (this.childNodes[j].className == "rec") {
            this.childNodes[j].style.visibility = "visible";
        }
        if (this.childNodes[j].className == "del") {
            this.childNodes[j].style.visibility = "visible";
        }
    }
}
function renameEmail(event) {
    if (event.target.classList.contains("rec")) {
        var mass = this.getElementsByTagName("div")[1];
        var text = prompt("Измените сообщение", mass.innerHTML);
        mass.innerHTML = text;
        changeMail(this, text);
    }
    if (event.target.classList.contains("del")) {
        deleteMail(this);
    }
}
function onKeyDown(event) {
    if (event.keyCode == 13 && event.shiftKey) {
        sendEmail();
        if (event.stopPropagation) {
            event.stopPropagation()
        } else {
            event.cancelBubble = true
        }
        document.getElementsByName("email")[0].value = "";
    }
}
function sendEmail() {
    var s = '';
    var email = document.getElementsByName("email")[0];
    var massString = email.value.split('\n');
    for (var i = 0; i < massString.length; i++) {
        s += massString[i];
        if (i != massString.length - 1) {
            s += "<br>";
        }
    }
    email.value = "";
    var name = document.getElementById("nameUser").textContent;
    var e = document.getElementsByClassName("mailHistory")[0];
    var dialogID = document.getElementsByClassName("partner")[0].getAttribute("dialogID");
    var task = createTask(name, s, dialogID, -1);
    var m = createItem(task);
    sendServlet(task, m, mailList.length);
    e.appendChild(m);
    mailList.push(task);
    e.scrollTop = e.scrollHeight;
    var messages = document.getElementsByClassName("mail")
    addListener(messages[messages.length - 1]);
}
function addListener(message) {
    message.addEventListener("mouseover", mouseOver);
    message.addEventListener("mouseout", mouseOut);
    message.addEventListener("click", renameEmail);
}
function sendServlet(task, message, index) {
    var req = new XMLHttpRequest();
    req.open("POST", "/ChatListener", true);
    req.send(JSON.stringify(task));
    req.onreadystatechange = function () {
        if (req.readyState == 4) {
            if (req.status == 200) {
                var mailID = parseInt(req.responseText);
                mailList[index].mailID = mailID;
                message.setAttribute("mailID", mailID);
            }
        }
    }
}
function createItem(task) {
    var temp = document.createElement('div');
    temp.innerHTML = '<div class="mail" data-task-id="id">\
    <div class="nameUser">sadasd</div>\
    <img class="rec" onclick="renameEmail()" \
    src = "styles/rec.gif"> </img>\
    <img class="del" src = "styles/del.png"> </img>\
    <br><br>\
    <div class="emailText"><div>';
    updateMail(temp.firstChild, task);
    return temp.firstChild;
}
function updateMail(divItem, task) {
    divItem.lastChild.innerHTML = task.text;
    divItem.getElementsByClassName("nameUser")[0].innerHTML = task.userName;
    divItem.setAttribute('dialogID', task.dialogID);
    divItem.setAttribute("mailID", task.mailID);
}
function createTask(user, text, dialogID, mailID) {
    return {
        text: text,
        userName: user,
        dialogID: dialogID,
        mailID: mailID,
        status: 0
    };
}
function upload(allMail) {
    var e = document.getElementsByClassName("mailHistory")[0];
    var t = document.getElementsByClassName("partner")[0].getAttribute("dialogID");
    var userName = document.getElementById("nameUser").innerHTML;
    for (var i = 0; i < allMail.length; i++) {
        if (allMail != mailList) {
            mailList.push(allMail[i]);
        }
        if (allMail[i].dialogID == t && allMail[i].userName != null) {
            var m = createItem(allMail[i]);
            e.appendChild(m);
            if (allMail[i].userName == userName) {
                addListener(m);
            }
        }
    }
    e.scrollTop = e.scrollHeight;
}
function uploadAllMail(allMail) {
    var e = document.getElementsByClassName("mailHistory")[0];
    e.innerHTML = "";
    upload(allMail);
}
function getIndexElement(dialogID, mailID) {
    for (var i = 0; mailList.length; i++) {
        if (mailList[i].mailID == mailID && mailList[i].dialogID == dialogID) {
            return i;
        }
    }
}
function deleteMail(mail) {
    var mailID = parseInt(mail.getAttribute("mailID"));
    var dialogID = parseInt(mail.getAttribute("dialogID"));
    var i = getIndexElement(dialogID, mailID);
    mailList[i].text = "message has been delete";
    mailList[i].status = 1;
    mail.getElementsByClassName("emailText")[0].innerHTML = "MESSAGE HAS BEEN DELETE";
    deleteMailServer(mailID, dialogID);
    addListener(mail);
}
function deleteMailServer(mailID, dialogID) {
    var task = createTask(0, 0, dialogID, mailID);
    var req = new XMLHttpRequest();
    req.open("DELETE", "/ChatListener");
    req.send(JSON.stringify(task));
}
function changeMail(mail, text) {
    var dialogID = parseInt(mail.getAttribute("dialogID"));
    var mailID = parseInt(mail.getAttribute("mailID"));
    var index = getIndexElement(dialogID, mailID);
    mailList[index].text = text;
    changeMailServer(mailID, dialogID, text);
}
function changeMailServer(mailID, dialogID, text) {
    var task = createTask("0", text, dialogID, mailID);
    var req = new XMLHttpRequest();
    req.open("PUT", "/ChatListener");
    req.send(JSON.stringify(task));
}
function connectServ() {
    var req = new XMLHttpRequest();
    req.open("GET", "/ChatListener");
    req.send();
    req.onreadystatechange = function () {
        if (req.readyState == 4) {
            var elem = document.getElementsByClassName("circle")[0];
            var textarea = document.getElementsByName("email")[0];
            if (req.status == 200) {
                connectServ();
                changeTextArea(textarea);
                elem.style.backgroundColor = 'green';
                var items = JSON.parse(req.responseText);
                if (items != null) {
                    if (items.update == 1) {
                        startServer();
                    }
                    else if (items[0].status == 1 || items[0].status == 2) {
                        changeMailInHistory(items[0].dialogID, items[0].mailID, items[0].text, items[0].status);
                    } else {
                        mailList.push(items);
                        upload(items);
                    }
                }
                return;
            }
            else {
                if (textarea.readOnly == false) {
                    textarea.readOnly = true;
                    textarea.backgroundColor = 'red';
                    textarea.value = "server is down, wait a bit";
                }
                elem.style.backgroundColor = 'red';
                window.setTimeout(testServer, 1000);
                return;
            }
        }
    }
}
function testServer() {
    var req = new XMLHttpRequest();
    req.open("POST", "/ChatListener");
    req.send("{\"test\":1}");
    req.onreadystatechange = function () {
        if (req.readyState == 4) {
            var elem = document.getElementsByClassName("circle")[0];
            var textarea = document.getElementsByName("email")[0];
            if (req.status == 200) {
                connectServ();
                changeTextArea(textarea);
                elem.style.backgroundColor = 'green';
                return;
            }
            else {
                if (textarea.readOnly == false) {
                    textarea.readOnly = true;
                    textarea.backgroundColor = 'red';
                    textarea.value = "server is down, wait a bit";
                }
                elem.style.backgroundColor = 'red';
                window.setTimeout(testServer, 1000);
                return;
            }
        }
    }
}
function changeTextArea(textArea) {
    if (textArea.readOnly == true) {
        textArea.readOnly = false;
        textArea.backgroundColor = "white";
        textArea.value = "";
    }
}
function changeMailInHistory(dialogID, mailID, text, status) {
    var i = getIndexElement(dialogID, mailID)
    mailList[i].text = text;
    mailList[i].status = status;
    if (dialogID == document.getElementsByClassName("partner")[0].getAttribute("dialogID")) {
        uploadAllMail(mailList);
    }
}
function startServer() {
    respFriend();
    respDialog();
    respListMail();
}
function respFriend() {
    var req = new XMLHttpRequest();
    req.open("POST", "/ChatListener");
    req.send(JSON.stringify(flagTask(1, null)));
    req.onreadystatechange = function () {
        if (req.readyState == 4) {
            if (req.status == 200) {
                var items = JSON.parse(req.responseText);
                if (items != null) {
                    updateFriend(items);
                }
            }
        }
    }
}
function respDialog() {
    var req = new XMLHttpRequest();
    req = new XMLHttpRequest();
    req.open("POST", "/ChatListener");
    req.send(JSON.stringify(flagTask(2, null)));
    req.onreadystatechange = function () {
        if (req.readyState == 4) {
            if (req.status == 200) {
                var items = JSON.parse(req.responseText);
                if (items != null) {
                    updateDialog(items);
                }
            }
        }
    }
}
function respListMail() {
    var req = new XMLHttpRequest();
    req = new XMLHttpRequest();
    req.open("POST", "/ChatListener");
    req.send(JSON.stringify(flagTask(3, null)));
    req.onreadystatechange = function () {
        if (req.readyState == 4) {
            if (req.status == 200) {
                var items = JSON.parse(req.responseText);
                if (items != null) {
                    updateListMail(items);
                }
            }
        }
    }
}
function updateFriend(items) {
    var friend = document.getElementsByClassName("friendList")[0];
    friend.innerHTML = "";
    for (var i = 0; i < items.length; i++) {
        var temp = document.createElement("div");
        temp.innerHTML = items[i].name;
        if (items[i].flag == "1") {
            var user = document.getElementById("nameUser");
            user.innerHTML = items[i].name;
        }
        else {
            temp.setAttribute("userID", items[i].userID);
            temp.setAttribute("class", "friend");
            friend.appendChild(temp);
            temp.addEventListener("click", addDialog)
        }
    }
}
function updateDialog(items) {
    var dial = document.getElementsByClassName("dialogList")[0];
    dial.innerHTML = "";
    for (var i = 0; i < items.length; i++) {
        var temp = document.createElement("div");
        temp.setAttribute("class", "dialogNode");
        temp.setAttribute("dialogID", items[i].dialogID);
        temp.innerHTML = items[i].massUsers;
        temp.addEventListener("click", changeDialog);
        dial.appendChild(temp);
    }
}
function updateListMail(items) {
    mailList = [];
    for (var i = 0; i < items.length; i++) {
        mailList.push(items[i]);
    }
}
function addDialog() {
    var userID = this.getAttribute("userID");
    var req = new XMLHttpRequest();
    req.open("POST", "/ChatListener");
    req.send(JSON.stringify(flagTask(4, userID)));
    req.onreadystatechange = function () {
        if (req.status == 4) {
            if (req.readyState == 200) {
                respDialog();
            }
        }
    }
}
function flagTask(flag, id, dialogID) {
    return {
        flag: flag,
        id: id,
        dialogID: dialogID
    };
}
function addNewUser() {
    var user = prompt("Введите логин:", "");
    var userID = -1;
    var users = document.getElementsByClassName("friendList")[0];
    for (var i = 0; i < users.childNodes.length; i++) {
        if (user == users.childNodes[i].innerHTML) {
            userID = parseInt(users.childNodes[i].getAttribute("userID"));
            break;
        }
    }
    var partner = document.getElementsByClassName("partner")[0];
    var dialogID = partner.getAttribute("dialogID");
    var req = new XMLHttpRequest();
    req.open("POST", "/ChatListener");
    req.send(JSON.stringify(flagTask(5, userID, dialogID)));
    req.onreadystatechange = function () {
        if (req.status == 4) {
            if (req.readyState == 200) {
                respDialog();
                partner.innerHTML += "<br>" + user;
            }
        }
    }
}
function changeDialog(event) {
    var partner = document.getElementsByClassName("partner")[0];
    partner.setAttribute("dialogID", this.getAttribute("dialogID"));
    partner.innerHTML = this.innerHTML;
    uploadAllMail(mailList);
}
