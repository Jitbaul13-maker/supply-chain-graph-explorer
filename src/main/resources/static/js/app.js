const API = "/api/graph";

let selectedDependencyId = null;
let searchTimer = null;
let overviewRequestId = 0;

const searchInput =
    document.getElementById("searchInput");


// ---------------------------------------------------------
// LIVE SEARCH
// ---------------------------------------------------------

searchInput.addEventListener("input", function () {

    clearTimeout(searchTimer);

    const query = this.value.trim();

    if (!query) {

        document
            .getElementById("searchResults")
            .innerHTML = "";

        return;
    }

    searchTimer = setTimeout(() => {
        search();
    }, 250);
});


// ---------------------------------------------------------
// SEARCH
// ---------------------------------------------------------

document
    .getElementById("searchButton")
    .addEventListener("click", search);

document
    .getElementById("searchInput")
    .addEventListener("keydown", function (event) {

        if (event.key === "Enter") {
            search();
        }
    });


async function search() {

    const query =
        document
            .getElementById("searchInput")
            .value
            .trim();

    if (!query) {
        return;
    }

    clearError();

    try {

        const response =
            await fetch(
                `${API}/search?q=${encodeURIComponent(query)}`
            );

        if (!response.ok) {

            let message =
                "Search failed";

            try {

                const error =
                    await response.json();

                message =
                    error.message || message;

            } catch (_) {
                // Keep fallback message.
            }

            throw new Error(message);
        }

        const results =
            await response.json();

        renderSearchResults(results);

    } catch (error) {

        showError(error.message);

    }
}


// ---------------------------------------------------------
// SEARCH RESULTS
// ---------------------------------------------------------

function renderSearchResults(results) {

    const container =
        document.getElementById("searchResults");

    container.innerHTML = "";

    if (results.length === 0) {

        container.innerHTML =
            `<div class="empty">
                No matching services or dependencies.
            </div>`;

        return;
    }

    results.forEach(node => {

        const element =
            document.createElement("div");

        element.className = "search-result";

        element.innerHTML = `
            <div class="search-result-main">

                <strong>
                    ${escapeHtml(node.name)}
                </strong>

                <span class="result-badge ${node.type.toLowerCase()}">
                    ${escapeHtml(node.type)}
                </span>

            </div>
        `;

        element.addEventListener("click", () => {

            document
                .getElementById("searchInput")
                .value = node.name;

            container.innerHTML = "";

            if (node.type === "Dependency") {

                loadDependency(
                    node.id,
                    node.name
                );

            } else {

                loadServiceDependencies(
                    node.id,
                    node.name
                );
            }
        });

        container.appendChild(element);
    });
}


// ---------------------------------------------------------
// DEPENDENCY OVERVIEW
// ---------------------------------------------------------

async function loadDependency(id, name) {

    selectedDependencyId = id;

    document
        .getElementById("targetSection")
        .classList.remove("hidden");

    document
        .getElementById("targetName")
        .textContent = name;


    /*
     * Immediately remove the previous result.
     *
     * This prevents the old dependency's data from
     * appearing to belong to the newly selected target.
     */

    clearAnalysis();

    clearError();

    showLoading();


    /*
     * Every overview request receives a unique id.
     *
     * If the user selects another dependency before
     * this request finishes, the older response is ignored.
     */

    const requestId = ++overviewRequestId;


    const refreshButton =
        document.getElementById("refreshButton");

    refreshButton.disabled = true;


    try {

        const response =
            await fetch(
                `${API}/overview/${encodeURIComponent(id)}`
            );


        if (!response.ok) {

            let message =
                "Unable to load graph";

            try {

                const error =
                    await response.json();

                message =
                    error.message || message;

            } catch (_) {
                // Keep fallback message.
            }

            throw new Error(message);
        }


        const data =
            await response.json();


        /*
         * Ignore stale responses.
         */

        if (requestId !== overviewRequestId) {
            return;
        }


        renderOverview(data);


    } catch (error) {

        if (requestId === overviewRequestId) {

            showError(
                error.message
            );
        }


    } finally {

        if (requestId === overviewRequestId) {

            hideLoading();

            refreshButton.disabled = false;
        }
    }
}


// ---------------------------------------------------------
// SERVICE SELECTION
// ---------------------------------------------------------

async function loadServiceDependencies(id, name) {

    clearError();

    showLoading();

    try {

        const response =
            await fetch(
                `${API}/dependencies/${encodeURIComponent(id)}`
            );

        if (!response.ok) {

            throw new Error(
                "Unable to load service dependencies"
            );
        }

        const dependencies =
            await response.json();


        const container =
            document.getElementById("searchResults");

        container.innerHTML = `
            <div class="result-type">
                Dependencies of
                ${escapeHtml(name)}
            </div>
        `;


        if (dependencies.length === 0) {

            container.innerHTML +=
                `<div class="empty">
                    No dependencies found.
                </div>`;

            return;
        }


        dependencies.forEach(dependency => {

            const element =
                document.createElement("div");

            element.className =
                "search-result";

            element.innerHTML = `
                <strong>
                    ${escapeHtml(dependency.name)}
                </strong>

                <div class="result-type">
                    Dependency
                </div>
            `;


            element.addEventListener(
                "click",
                () => {

                    loadDependency(
                        dependency.id,
                        dependency.name
                    );
                }
            );


            container.appendChild(element);
        });


    } catch (error) {

        showError(error.message);

    } finally {

        hideLoading();
    }
}


// ---------------------------------------------------------
// OVERVIEW RENDERING
// ---------------------------------------------------------

function renderOverview(data) {

    document
        .getElementById("serviceCount")
        .textContent =
        data.affectedServices.length;


    document
        .getElementById("ownerCount")
        .textContent =
        data.owners.length;


    document
        .getElementById("regionCount")
        .textContent =
        data.regions.length;


    document
        .getElementById("alternativeCount")
        .textContent =
        data.alternatives.length;


    renderBlastRadius(
        data.affectedServices,
        data.incidents
    );

    renderOwners(
        data.owners
    );

    renderRegions(
        data.regions
    );

    renderAlternatives(
        data.alternatives
    );
}


// ---------------------------------------------------------
// BLAST RADIUS
// ---------------------------------------------------------

function renderBlastRadius(services, incidents) {

    const container =
        document.getElementById("blastRadius");

    container.innerHTML = "";


    if (services.length === 0) {

        container.innerHTML =
            `<div class="empty">
                No affected services.
            </div>`;

        return;
    }


    const targetName =
        document
            .getElementById("targetName")
            .textContent;


    const graph =
        document.createElement("div");

    graph.className =
        "graph-container";


    const targetNode =
        document.createElement("div");

    targetNode.className =
        "graph-target-node";


    targetNode.innerHTML = `
        <span class="graph-node-type">
            DEPENDENCY
        </span>

        <strong>
            ${escapeHtml(targetName)}
        </strong>
    `;


    graph.appendChild(targetNode);


    const connections =
        document.createElement("div");

    connections.className =
        "graph-connections";


    services.forEach(service => {

        const connection =
            document.createElement("div");

        connection.className =
            "graph-connection";


        const line =
            document.createElement("div");

        line.className =
            "graph-line";


        const serviceNode =
            document.createElement("div");

        serviceNode.className =
            "graph-service-node";

        const serviceIncidents =
                incidents.filter(
                    incident => incident.service === service.service
                );


        serviceNode.innerHTML = `
            <div>

                <span class="graph-node-type">
                    SERVICE
                </span>

                <strong>
                    ${escapeHtml(service.service)}
                </strong>

            </div>

            <span class="hop-badge">
                ${service.hops}
                hop${service.hops === 1 ? "" : "s"}
            </span>

            ${serviceIncidents.map(incident => `
                <div class="incident-badge ${incident.severity.toLowerCase()}">
                    <span>INCIDENT</span>
                    <strong>${escapeHtml(incident.incident)}</strong>
                    <small>${escapeHtml(incident.severity)}</small>
                </div>
            `).join("")}
        `;

        connection.appendChild(line);

        connection.appendChild(
            serviceNode
        );

        connections.appendChild(
            connection
        );
    });


    graph.appendChild(
        connections
    );

    container.appendChild(
        graph
    );
}


// ---------------------------------------------------------
// OWNERS
// ---------------------------------------------------------

function renderOwners(owners) {

    const container =
        document.getElementById("owners");

    container.innerHTML = "";


    if (owners.length === 0) {

        container.innerHTML =
            `<div class="empty">
                No owners found.
            </div>`;

        return;
    }


    owners.forEach(owner => {

        const row =
            document.createElement("div");

        row.className =
            "data-row";


        row.innerHTML = `
            <span>
                ${escapeHtml(owner.service)}
            </span>

            <span class="badge">
                ${escapeHtml(owner.owner)}
            </span>
        `;


        container.appendChild(row);
    });
}


// ---------------------------------------------------------
// REGIONS
// ---------------------------------------------------------

function renderRegions(regions) {

    const container =
        document.getElementById("regions");

    container.innerHTML = "";


    if (regions.length === 0) {

        container.innerHTML =
            `<div class="empty">
                No deployment information.
            </div>`;

        return;
    }


    regions.forEach(region => {

        const row =
            document.createElement("div");

        row.className =
            "data-row";


        row.innerHTML = `
            <span>
                ${escapeHtml(region.service)}
            </span>

            <span class="badge">
                ${escapeHtml(region.environment)}
                ·
                ${escapeHtml(region.region)}
            </span>
        `;


        container.appendChild(row);
    });
}


// ---------------------------------------------------------
// ALTERNATIVES
// ---------------------------------------------------------

function renderAlternatives(alternatives) {

    const container =
        document.getElementById("alternatives");

    container.innerHTML = "";


    if (alternatives.length === 0) {

        container.innerHTML =
            `<div class="empty">
                No alternative paths found.
            </div>`;

        return;
    }


    alternatives.forEach(alternative => {

        const row =
            document.createElement("div");

        row.className =
            "data-row";


        row.innerHTML = `
            <span>
                ${escapeHtml(
                    alternative.fromDependency
                )}

                →

                ${escapeHtml(
                    alternative.toDependency
                )}
            </span>

            <span class="badge">
                ${escapeHtml(
                    alternative.relationship
                )}
            </span>
        `;


        container.appendChild(row);
    });
}


// ---------------------------------------------------------
// REFRESH
// ---------------------------------------------------------

document
    .getElementById("refreshButton")
    .addEventListener("click", async () => {

        if (!selectedDependencyId) {
            return;
        }


        const name =
            document
                .getElementById("targetName")
                .textContent;


        await loadDependency(
            selectedDependencyId,
            name
        );
    });


// ---------------------------------------------------------
// CLEAR OLD ANALYSIS
// ---------------------------------------------------------

function clearAnalysis() {

    document
        .getElementById("serviceCount")
        .textContent = "—";

    document
        .getElementById("ownerCount")
        .textContent = "—";

    document
        .getElementById("regionCount")
        .textContent = "—";

    document
        .getElementById("alternativeCount")
        .textContent = "—";


    document
        .getElementById("blastRadius")
        .innerHTML = "";

    document
        .getElementById("owners")
        .innerHTML = "";

    document
        .getElementById("regions")
        .innerHTML = "";

    document
        .getElementById("alternatives")
        .innerHTML = "";
}


// ---------------------------------------------------------
// UI HELPERS
// ---------------------------------------------------------

function showLoading() {

    document
        .getElementById("loading")
        .classList.remove("hidden");
}


function hideLoading() {

    document
        .getElementById("loading")
        .classList.add("hidden");
}


function showError(message) {

    const element =
        document.getElementById("error");

    element.textContent =
        message;

    element.classList.remove(
        "hidden"
    );
}


function clearError() {

    document
        .getElementById("error")
        .classList.add("hidden");
}


function escapeHtml(value) {

    return String(value)

        .replaceAll(
            "&",
            "&amp;"
        )

        .replaceAll(
            "<",
            "&lt;"
        )

        .replaceAll(
            ">",
            "&gt;"
        )

        .replaceAll(
            '"',
            "&quot;"
        )

        .replaceAll(
            "'",
            "&#039;"
        );
}