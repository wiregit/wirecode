// Copyright (C) 2005 Arkadiy Vertleyb, Peder Holt.
// Use, modification and distribution is subject to the Boost Software
// License, Version 1.0. (http://www.boost.org/LICENSE_1_0.txt)

#ifndef BOOST_TYPEOF_STD_vector_hpp_INCLUDED
#define BOOST_TYPEOF_STD_vector_hpp_INCLUDED

#include <vector>
#include <boost/typeof/typeof.hpp>
#include <boost/typeof/std/memory.hpp>

#include BOOST_TYPEOF_INCREMENT_REGISTRATION_GROUP()

BOOST_TYPEOF_REGISTER_TEMPLATE(std::vector, 1)
BOOST_TYPEOF_REGISTER_TEMPLATE(std::vector, 2)

#endif//BOOST_TYPEOF_STD_vector_hpp_INCLUDED
