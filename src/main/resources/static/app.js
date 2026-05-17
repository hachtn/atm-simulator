const state = {
    token: null,
    account: null,
    pinInput: "",
    pinTry: 0,
    selectedAmount: 0,
    pendingAccountNumber: ""
};

const screenTitle = document.getElementById("screenTitle");
const screenTime = document.getElementById("screenTime");
const statusText = document.getElementById("statusText");
const accountNumberInput = document.getElementById("accountNumberInput");
const welcomeErr = document.getElementById("welcomeErr");
const pinErr = document.getElementById("pinErr");
const pinAccountLabel = document.getElementById("pinAccountLabel");
const menuAccountHolder = document.getElementById("menuAccountHolder");
const menuAccountNumber = document.getElementById("menuAccountNumber");
const actionView = document.getElementById("vAction");
const resultView = document.getElementById("vResult");

function tick() {
    const now = new Date();
    screenTime.textContent = now.toLocaleTimeString("en-GB", {
        hour: "2-digit",
        minute: "2-digit"
    });
}

function setTitle(text) {
    screenTitle.textContent = text;
}

function show(viewId) {
    ["vWelcome", "vPin", "vMenu", "vAction", "vResult"].forEach((id) => {
        document.getElementById(id).classList.remove("active");
    });
    document.getElementById(viewId).classList.add("active");
}

function maskAccountNumber(accountNumber) {
    if (!accountNumber) {
        return "****";
    }
    const last4 = accountNumber.slice(-4);
    return `**** ${last4}`;
}

function formatCurrency(amount) {
    return new Intl.NumberFormat("ja-JP", {
        style: "currency",
        currency: "JPY",
        maximumFractionDigits: 0
    }).format(amount);
}

function updateStatus(message) {
    statusText.textContent = message;
}

function updateMenuAccount() {
    if (!state.account) {
        menuAccountHolder.textContent = "Guest";
        menuAccountNumber.textContent = "****";
        return;
    }
    menuAccountHolder.textContent = state.account.accountHolder;
    menuAccountNumber.textContent = maskAccountNumber(state.account.accountNumber);
}

function renderDots() {
    for (let index = 0; index < 4; index += 1) {
        const dot = document.getElementById(`pd${index}`);
        dot.classList.toggle("filled", index < state.pinInput.length);
    }
}

function resetPinEntry() {
    state.pinInput = "";
    renderDots();
}

function appendPin(digit) {
    if (state.pinInput.length >= 4) {
        return;
    }
    state.pinInput += digit;
    renderDots();
    if (state.pinInput.length === 4) {
        setTimeout(confirmPin, 150);
    }
}

function clearPin() {
    resetPinEntry();
    pinErr.textContent = "";
}

async function api(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers ?? {})
    };

    if (state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }

    const response = await fetch(path, {
        ...options,
        headers
    });

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json")
        ? await response.json()
        : null;

    if (!response.ok) {
        throw new Error(payload?.message || "Request failed");
    }

    return payload;
}

function requireSession() {
    if (!state.token || !state.account) {
        throw new Error("Please sign in again");
    }
}

function showResult(ok, title, subtext, amountText = "", amountClass = "") {
    const icon = ok
        ? '<i class="ti ti-circle-check" style="font-size:32px; color:#2a8a2a;"></i>'
        : '<i class="ti ti-alert-circle" style="font-size:32px; color:#c8102e;"></i>';
    const amountBlock = amountText
        ? `<div class="result-amt ${amountClass}">${amountText}</div>`
        : "";
    resultView.innerHTML = `
        <span class="result-icon">${icon}</span>
        <div class="result-msg">${title}</div>
        <div class="result-sub">${subtext}</div>
        ${amountBlock}
        <div class="bal-remain">Balance: ${formatCurrency(state.account?.balance ?? 0)}</div>
        <button class="btn-home" onclick="showMenu()"><i class="ti ti-home"></i> Back to Menu</button>
    `;
    setTitle(ok ? "Completed" : "Notice");
    show("vResult");
}

function showError(error) {
    showResult(false, "Request failed", error.message || String(error));
}

function renderAction(content, title) {
    actionView.innerHTML = content;
    setTitle(title);
    show("vAction");
}

function getSelectedAmount() {
    const customAmountInput = document.getElementById("customAmt");
    if (customAmountInput && customAmountInput.value) {
        return Number(customAmountInput.value);
    }
    return state.selectedAmount;
}

function selectAmount(amount, element) {
    state.selectedAmount = amount;
    document.querySelectorAll(".amt-btn").forEach((button) => button.classList.remove("selected"));
    element.classList.add("selected");
    const customAmountInput = document.getElementById("customAmt");
    if (customAmountInput && amount > 0) {
        customAmountInput.value = "";
    }
}

function clearSelectedAmount() {
    state.selectedAmount = 0;
    document.querySelectorAll(".amt-btn").forEach((button) => button.classList.remove("selected"));
}

function showMenu() {
    updateMenuAccount();
    setTitle("Choose Transaction");
    show("vMenu");
}

function insertCard() {
    const accountNumber = accountNumberInput.value.trim();
    if (!accountNumber) {
        welcomeErr.textContent = "Enter an account number first.";
        return;
    }

    welcomeErr.textContent = "";
    pinErr.textContent = "";
    state.pinTry = 0;
    state.pendingAccountNumber = accountNumber;
    resetPinEntry();
    pinAccountLabel.textContent = `Account ${maskAccountNumber(accountNumber)}`;
    setTitle("Enter PIN");
    show("vPin");
}

async function confirmPin() {
    if (state.pinInput.length !== 4) {
        pinErr.textContent = "Enter a 4-digit PIN.";
        return;
    }

    try {
        const loginResponse = await api("/api/atm/login", {
            method: "POST",
            body: JSON.stringify({
                accountNumber: state.pendingAccountNumber,
                pin: state.pinInput
            })
        });

        state.token = loginResponse.token;
        state.account = loginResponse.account;
        state.pinTry = 0;
        resetPinEntry();
        pinErr.textContent = "";
        updateStatus("Session active");
        showMenu();
    } catch (error) {
        state.pinTry += 1;
        resetPinEntry();
        if (state.pinTry >= 3) {
            pinErr.textContent = "PIN entered incorrectly 3 times. Session cancelled.";
            setTimeout(logout, 1800);
        } else {
            pinErr.textContent = `${error.message}. ${3 - state.pinTry} attempt(s) left.`;
        }
    }
}

async function loadBalance() {
    requireSession();
    const response = await api("/api/atm/balance");
    state.account.balance = response.balance;
    return response.balance;
}

async function loadTransactions() {
    requireSession();
    return api("/api/atm/transactions");
}

async function openBalanceView() {
    try {
        const balance = await loadBalance();
        renderAction(`
            <div class="acct-box">
                <div class="acct-label">Account</div>
                <div class="acct-num">${state.account.accountHolder} · ${maskAccountNumber(state.account.accountNumber)}</div>
            </div>
            <div class="acct-box">
                <div class="acct-label">Available Balance</div>
                <div class="bal-amount">${new Intl.NumberFormat("ja-JP").format(balance)}<span class="bal-unit">JPY</span></div>
            </div>
            <div class="action-bar">
                <button class="btn-back" onclick="showMenu()">Back</button>
            </div>
        `, "Balance");
    } catch (error) {
        showError(error);
    }
}

function openWithdrawView() {
    state.selectedAmount = 0;
    renderAction(`
        <div class="action-title">Choose an amount</div>
        <div class="amt-grid">
            <button class="amt-btn" onclick="selectAmount(10000, this)">10,000 JPY</button>
            <button class="amt-btn" onclick="selectAmount(20000, this)">20,000 JPY</button>
            <button class="amt-btn" onclick="selectAmount(30000, this)">30,000 JPY</button>
            <button class="amt-btn" onclick="selectAmount(50000, this)">50,000 JPY</button>
            <button class="amt-btn" onclick="selectAmount(100000, this)">100,000 JPY</button>
            <button class="amt-btn" onclick="selectAmount(0, this)">Other</button>
        </div>
        <div class="custom-row">
            <input type="number" id="customAmt" placeholder="Enter amount..." oninput="clearSelectedAmount()" />
        </div>
        <div class="bal-remain-inline">Balance: ${formatCurrency(state.account.balance)}</div>
        <div class="action-bar">
            <button class="btn-back" onclick="showMenu()">Back</button>
            <button class="btn-confirm" onclick="submitWithdraw()">Confirm</button>
        </div>
    `, "Withdraw");
}

async function submitWithdraw() {
    try {
        const amount = getSelectedAmount();
        if (!amount || amount <= 0) {
            throw new Error("Select or enter a withdrawal amount");
        }

        const response = await api("/api/atm/withdraw", {
            method: "POST",
            body: JSON.stringify({ amount })
        });

        state.account.balance = response.balance;
        showResult(true, "Withdrawal complete", "Please collect your cash.", `-${formatCurrency(amount)}`, "minus");
    } catch (error) {
        showError(error);
    }
}

async function openTransactionsView() {
    try {
        const transactions = await loadTransactions();
        const content = transactions.length
            ? transactions.map((transaction) => `
                <div class="history-item">
                    <div class="history-top">
                        <span class="history-type">${transaction.type}</span>
                        <span class="history-amount">${formatCurrency(transaction.amount)}</span>
                    </div>
                    <div class="history-meta">${new Date(transaction.transactionTime).toLocaleString("en-GB")}</div>
                    <div class="history-meta">Balance after: ${formatCurrency(transaction.balanceAfter)}</div>
                    <div class="history-meta">${transaction.description || transaction.branchCode || "-"}</div>
                </div>
            `).join("")
            : '<div class="empty-state">No transactions yet for this account.</div>';

        renderAction(`
            <div class="history-list">${content}</div>
            <div class="action-bar">
                <button class="btn-back" onclick="showMenu()">Back</button>
            </div>
        `, "Transactions");
    } catch (error) {
        showError(error);
    }
}

function openTransferView() {
    renderAction(`
        <div class="field-label">Target Account Number</div>
        <input class="jp-input" id="tfAcct" type="text" placeholder="7654321" />
        <div class="field-label">Target Bank</div>
        <input class="jp-input" id="tfBank" type="text" value="Nihon Bank" />
        <div class="field-label">Amount (JPY)</div>
        <input class="jp-input" id="tfAmt" type="number" placeholder="5000" />
        <div class="field-label">Memo</div>
        <input class="jp-input" id="tfNote" type="text" placeholder="Rent, allowance, etc." />
        <div class="action-bar">
            <button class="btn-back" onclick="showMenu()">Back</button>
            <button class="btn-confirm" onclick="submitTransfer()">Transfer</button>
        </div>
    `, "Transfer");
}

async function submitTransfer() {
    try {
        const targetAccountNumber = document.getElementById("tfAcct").value.trim();
        const targetBankName = document.getElementById("tfBank").value.trim();
        const note = document.getElementById("tfNote").value.trim();
        const amount = Number(document.getElementById("tfAmt").value);

        if (!targetAccountNumber) {
            throw new Error("Enter a target account number");
        }
        if (!amount || amount <= 0) {
            throw new Error("Enter a valid transfer amount");
        }

        const response = await api("/api/atm/transfer", {
            method: "POST",
            body: JSON.stringify({ targetAccountNumber, targetBankName, note, amount })
        });

        state.account.balance = response.balance;
        showResult(true, "Transfer complete", `Sent money to ${targetAccountNumber}.`, `-${formatCurrency(amount)}`, "minus");
    } catch (error) {
        showError(error);
    }
}

function openTopUpView() {
    state.selectedAmount = 0;
    renderAction(`
        <div class="notice-box"><i class="ti ti-info-circle"></i>Charge a stored-value service from your account balance.</div>
        <div class="field-label">Top-up Target</div>
        <input class="jp-input" id="tuTarget" type="text" placeholder="Suica / PASMO / Wallet" />
        <div class="action-title">Choose an amount</div>
        <div class="amt-grid">
            <button class="amt-btn" onclick="selectAmount(1000, this)">1,000 JPY</button>
            <button class="amt-btn" onclick="selectAmount(2000, this)">2,000 JPY</button>
            <button class="amt-btn" onclick="selectAmount(3000, this)">3,000 JPY</button>
            <button class="amt-btn" onclick="selectAmount(5000, this)">5,000 JPY</button>
            <button class="amt-btn" onclick="selectAmount(10000, this)">10,000 JPY</button>
            <button class="amt-btn" onclick="selectAmount(20000, this)">20,000 JPY</button>
        </div>
        <div class="action-bar">
            <button class="btn-back" onclick="showMenu()">Back</button>
            <button class="btn-confirm" onclick="submitTopUp()">Top-up</button>
        </div>
    `, "Top-up");
}

async function submitTopUp() {
    try {
        const target = document.getElementById("tuTarget").value.trim();
        const amount = state.selectedAmount;

        if (!target) {
            throw new Error("Enter a top-up target");
        }
        if (!amount || amount <= 0) {
            throw new Error("Select a top-up amount");
        }

        const response = await api("/api/atm/topup", {
            method: "POST",
            body: JSON.stringify({ target, amount })
        });

        state.account.balance = response.balance;
        showResult(true, "Top-up complete", `Charged ${target}.`, `-${formatCurrency(amount)}`, "minus");
    } catch (error) {
        showError(error);
    }
}

function openChangePinView() {
    renderAction(`
        <div class="field-label">Current PIN</div>
        <input class="jp-input" id="cpOld" type="password" maxlength="4" placeholder="1234" />
        <div class="field-label">New PIN</div>
        <input class="jp-input" id="cpNew" type="password" maxlength="4" placeholder="5678" />
        <div class="field-label">Confirm New PIN</div>
        <input class="jp-input" id="cpCfm" type="password" maxlength="4" placeholder="5678" />
        <div class="pin-err pin-err-left" id="cpErr"></div>
        <div class="action-bar">
            <button class="btn-back" onclick="showMenu()">Back</button>
            <button class="btn-confirm" onclick="submitChangePin()">Change PIN</button>
        </div>
    `, "Change PIN");
}

async function submitChangePin() {
    const currentPin = document.getElementById("cpOld").value.trim();
    const newPin = document.getElementById("cpNew").value.trim();
    const confirmPinValue = document.getElementById("cpCfm").value.trim();
    const errorBox = document.getElementById("cpErr");

    try {
        if (newPin !== confirmPinValue) {
            throw new Error("PIN confirmation does not match");
        }

        await api("/api/atm/change-pin", {
            method: "POST",
            body: JSON.stringify({ currentPin, newPin })
        });

        showResult(true, "PIN changed", "Your ATM PIN has been updated.");
    } catch (error) {
        errorBox.textContent = error.message;
    }
}

async function logout() {
    try {
        if (state.token) {
            await api("/api/atm/logout", { method: "POST" });
        }
    } catch (error) {
        console.warn(error);
    } finally {
        state.token = null;
        state.account = null;
        state.pendingAccountNumber = "";
        state.pinTry = 0;
        state.selectedAmount = 0;
        resetPinEntry();
        accountNumberInput.value = "";
        welcomeErr.textContent = "";
        pinErr.textContent = "";
        updateStatus("Online");
        updateMenuAccount();
        setTitle("Welcome");
        show("vWelcome");
    }
}

tick();
setInterval(tick, 30000);
updateMenuAccount();
