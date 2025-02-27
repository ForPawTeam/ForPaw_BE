from .animal import (
    find_animals_without_introduction,
    generate_animal_introduction,
    update_animal_introductions,
    hybrid_recommendation
)

from .cb import (
    update_animal_similarity_data,
    get_cb_candidates,
    init_redis as init_redis_cb
)

from .cf import (
    train_cf_model,
    build_cf_recommendations,
    store_cf_results_in_redis,
    get_cf_candidates,
    init_redis as init_redis_cf
)
