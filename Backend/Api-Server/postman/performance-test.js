/**
 * Performance Test Script for Api-Server Caching
 *
 * Usage (with Newman):
 *   npm install -g newman
 *   newman run Api-Server.postman_collection.json -e environment.json --iteration-count 10 --delay-request 100
 *
 * Manual Test Steps:
 * 1. Start Api-Server (port 8088)
 * 2. Import Api-Server.postman_collection.json into Postman
 * 3. Run "Get Skill Categories (Caching Test)" 10 times
 * 4. Observe response times:
 *    - First call: ~100-500ms (DB query)
 *    - Subsequent calls: ~1-10ms (L1 cache hit)
 *    - After 10 seconds: ~10-50ms (L2 cache hit)
 *    - After 10 minutes: ~100-500ms (DB query again)
 */

// Postman Test Script (add to Tests tab)
pm.test("Response time is acceptable", function () {
    const responseTime = pm.response.responseTime;
    const iteration = pm.info.iteration;

    if (iteration === 0) {
        // First call - DB query
        pm.expect(responseTime).to.be.below(1000);
        console.log(`First call (DB): ${responseTime}ms`);
    } else if (iteration < 10) {
        // Subsequent calls - L1 cache
        pm.expect(responseTime).to.be.below(50);
        console.log(`L1 cache hit: ${responseTime}ms`);
    }
});

pm.test("Response has skill categories", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.data.skillCategories).to.be.an('array');
    pm.expect(jsonData.data.skillCategories.length).to.be.above(0);
});

// Save response time for analysis
if (!pm.globals.has('responseTimes')) {
    pm.globals.set('responseTimes', JSON.stringify([]));
}

const responseTimes = JSON.parse(pm.globals.get('responseTimes'));
responseTimes.push({
    iteration: pm.info.iteration,
    responseTime: pm.response.responseTime,
    timestamp: new Date().toISOString()
});
pm.globals.set('responseTimes', JSON.stringify(responseTimes));

// Print summary on last iteration
if (pm.info.iteration === pm.info.iterationCount - 1) {
    console.log("\n=== Performance Summary ===");
    const times = JSON.parse(pm.globals.get('responseTimes'));
    const avg = times.reduce((sum, t) => sum + t.responseTime, 0) / times.length;
    const min = Math.min(...times.map(t => t.responseTime));
    const max = Math.max(...times.map(t => t.responseTime));

    console.log(`Total iterations: ${times.length}`);
    console.log(`Average response time: ${avg.toFixed(2)}ms`);
    console.log(`Min response time: ${min}ms`);
    console.log(`Max response time: ${max}ms`);
    console.log(`First call (DB): ${times[0].responseTime}ms`);
    console.log(`Last call (cache): ${times[times.length - 1].responseTime}ms`);
    console.log(`Cache speedup: ${(times[0].responseTime / avg).toFixed(2)}x`);

    // Clear for next run
    pm.globals.unset('responseTimes');
}
