var mailList = [];
function run() {
    startServer();
    connectServ();
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
function sendEmail() {
    var s = '';
    var massString = document.getElementsByName("email")[0].value.split('\n');
    for (var i = 0; i < massString.length; i++) {
        s += massString[i];
        if (i != massString.length - 1) {
            s += "<br>";
        }
    }
    document.getElementsByName("email")[0].value = "";
    var name = document.getElementById("nameUser").textContent;
    var e = document.getElementsByClassName("mailHistory")[0];
    var dialogID = document.getElementsByClassName("partner")[0].getAttribute("dialogID");
    var task = createTask(name, s, dialogID);
    var m = createItem(task);
    sendServlet(task, m, mailList.length);
    e.appendChild(m);
    mailList.push(task);
    e.scrollTop = e.scrollHeight;
    var messages = document.getElementsByClassName("mail");
    messages[messages.length - 1].addEventListener("mouseover", mouseOver);
    messages[messages.length - 1].addEventListener("mouseout", mouseOut);
    messages[messages.length - 1].addEventListener("click", renameEmail);
}
function sendServlet(task, message, index) {
    var req = new XMLHttpRequest();
    req.open("POST", "/ChatListener");
    req.onreadystatechange = function () {
        if (req.status == 4) {
            if (req.readyState == 200) {
                var mailID = parseInt(req.responseText);
                mailList[index].mailID = mailID;
                message.setAttribute("mailID", mailID);
            }
        }
    }
    req.send(JSON.stringify(task));
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
    var text = divItem.lastChild;
    text.innerHTML = task.text;
    var name = divItem.getElementsByClassName("nameUser")[0];
    name.innerHTML = task.userName;
    divItem.setAttribute('dialogID', task.id);
}
function createTask(user, mailText, ID) {
    return {
        text: mailText,
        userName: user,
        dialogID: ID,
        mailID: -1
    };
}
function store(list) {
    if (typeof(Storage) == "undefined") {
        alert('localStorage is not accessible');
        return;
    }
    localStorage.setItem("taskList", JSON.stringify(list))
}
function storeUser(user) {
    if (typeof(Storage) == "undefined") {
        alert('localStorage is not accessible');
        return;
    }
    localStorage.setItem("username", user);
}
function upload(allMail) {
    var e = document.getElementsByClassName("mailHistory")[0];
    var t = document.getElementsByClassName("partner")[0].getAttribute("dialogID");
    for (var i = 0; i < allMail.length; i++) {
        if (allMail != mailList) {
            mailList.push(allMail[i]);
        }
        if (allMail[i].dialogID == t && allMail[i].userName != null) {
            var m = createItem(allMail[i]);
            e.appendChild(m);
            m.addEventListener("mouseover", mouseOver);
            m.addEventListener("mouseout", mouseOut);
            m.addEventListener("click", renameEmail);
        }
    }
    e.scrollTop = e.scrollHeight;
}
function uploadAllMail(allMail) {
    var e = document.getElementsByClassName("mailHistory")[0];
    e.innerHTML = "";
    upload(allMail);
}
function deleteMail(mail) {
    var index = -1;
    var r = mail.getAttribute("data-task-id");
    for (var i = 0; mailList.length; i++) {
        if (mailList[i].id == parseInt(r)) {
            index = i;
            break;
        }
    }
    mailList.splice(index, 1);
    store(mailList);
    mail.parentNode.removeChild(mail);
}
function changeMail(mail, text) {
    var index = -1;
    var r = mail.getAttribute("data-task-id");
    for (var i = 0; mailList.length; i++) {
        if (mailList[i].id == parseInt(r)) {
            index = i;
            break;
        }
    }
    mailList[index].text = text;
    store(mailList);
}
function connectServ() {
    var req = new XMLHttpRequest();
    req.open("GET", "/ChatListener");
    req.send();
    req.onreadystatechange = function () {
        if (req.readyState == 4) {
            var elem = document.getElementsByClassName("circle")[0];
            if (req.status == 0) {
                elem.style.backgroundColor = 'green';
                connectServ();
            }
            if (req.status == 200) {
                connectServ();
                elem.style.backgroundColor = 'green';
                var items = JSON.parse(req.responseText);
                if (items != null) {
                    if (items.update == 1) {
                        startServer();
                    }
                    else {
                        mailList.push(items);
                        upload(items);
                    }
                }
                return;
            }
            else {
                elem.style.backgroundColor = 'red';
                window.setTimeout(connectServ, 100);
                return;
            }
        }
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
        var flag = items[i].flag;
        if (flag == "1") {
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
function addDialog(event) {
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
